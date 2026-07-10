package com.bainian.memory;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.data.TodoRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.ui.CalendarPage;
import com.bainian.memory.ui.CenturyPage;
import com.bainian.memory.ui.MemoryFeedPage;
import com.bainian.memory.ui.PageHost;
import com.bainian.memory.ui.ProfilePage;
import com.bainian.memory.ui.RecordEditDialog;
import com.bainian.memory.ui.TimelinePage;
import com.bainian.memory.ui.TodoDetailPage;
import com.bainian.memory.ui.TodoListPage;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.view.TabIconView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Consumer;

/**
 * App 唯一 Activity：负责整体外壳（状态栏适配、底部导航栏）与三个 Tab 之间的切换调度。
 *
 * 信息架构（2.1）：
 *   底部保留三个最高频的 tab —— 回忆 / 时间线 / 计划，让高频操作都能一步直达；
 *   「计划」tab 展示全部 TODO 计划列表，点进某个计划查看任务详情时，
 *   才以「次级页面」形式 overlay 浮现（带返回箭头），因为这一层还有更细的操作。
 *   「日历」「我的」「百年」仍不占用底部导航位置，而是作为「次级页面」以 overlay 形式浮现在
 *   主内容之上（分别从首页图标 / 头像 / 「我的」页进入），点击返回箭头即可退出。
 *   其中「百年」是低频但情感浓度很高的页面——不适合常驻底部抢注意力，
 *   更适合作为一次「安静地回望人生」的主动探访，因此收进「我的」页里。
 *
 * 每个 Tab／次级页面的具体 UI 与逻辑分别在 ui 包下实现。
 */
public class MainActivity extends Activity implements PageHost {
    private final String[] tabLabels = {"回忆", "时间线", "计划"};

    private RecordRepository repository;
    private RecordEditDialog recordEditDialog;
    private TodoRepository todoRepository;

    private LinearLayout root;
    private FrameLayout content;
    private FrameLayout overlay;
    private LinearLayout navWrapper;
    private LinearLayout nav;
    private LinearLayout[] navItems;
    private TabIconView[] navIcons;
    private TextView[] navLabels;
    private View[] navDots;          // 选中态底部小圆点指示器
    private ValueAnimator[] itemAnimators;

    private LocalDate selectedDate = LocalDate.now();
    private YearMonth visibleMonth = YearMonth.now();
    private int currentTab = 0;

    private static final int PICK_AVATAR_REQUEST = 702;
    private Consumer<String> pendingAvatarCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new RecordRepository(this);
        todoRepository = new TodoRepository(this);
        recordEditDialog = new RecordEditDialog(this, repository, this::refreshCurrentTab);
        buildShell();
        showFeed();
    }

    // ═══════════════════════════════════════════
    //  Shell — 整体框架 + 底部导航栏
    // ═══════════════════════════════════════════

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiKit.getColorByName(this, "bg"));

        // 状态栏安全区：让内容顶部与状态栏之间留有舒适间距
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarH = insets.getSystemWindowInsetTop();
            root.setPadding(0, statusBarH, 0, 0);
            return insets;
        });

        // content 与 overlay 叠放：overlay 用于承载「日历」「我的」等次级页面
        FrameLayout stage = new FrameLayout(this);
        root.addView(stage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        content = new FrameLayout(this);
        stage.addView(content, matchParams());

        overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setBackgroundColor(UiKit.getColorByName(this, "bg"));
        stage.addView(overlay, matchParams());

        // 底部导航栏 —— 浮动胶囊 + 竖排图标/文字/指示器
        navWrapper = new LinearLayout(this);
        navWrapper.setPadding(dp(16), dp(4), dp(16), dp(12));
        navWrapper.setBackgroundColor(UiKit.getColorByName(this, "bg"));
        root.addView(navWrapper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(84)));

        // 胶囊容器
        FrameLayout capsule = new FrameLayout(this);
        capsule.setBackground(UiKit.getDrawableByName(this, "card_elevated"));
        float navRadius = dp(28);
        capsule.setElevation(dp(6));
        capsule.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), navRadius);
            }
        });
        capsule.setClipToOutline(true);
        navWrapper.addView(capsule, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        capsule.addView(nav, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        navItems = new LinearLayout[tabLabels.length];
        navIcons = new TabIconView[tabLabels.length];
        navLabels = new TextView[tabLabels.length];
        navDots = new View[tabLabels.length];
        itemAnimators = new ValueAnimator[tabLabels.length];

        int accentColor = UiKit.getColorByName(this, "accent");
        int mutedColor = UiKit.getColorByName(this, "muted_light");

        for (int i = 0; i < tabLabels.length; i++) {
            final int index = i;
            boolean active = i == 0;

            // 每个 tab 项：竖排 图标 / 文字 / 小圆点指示器
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(4), dp(4), dp(4));
            item.setOnClickListener(v -> {
                closeOverlay();
                switchTab(index);
            });

            // 图标
            TabIconView icon = new TabIconView(this);
            icon.setKind(i);
            icon.setSize(dp(19));
            icon.setColor(active ? accentColor : mutedColor);
            item.addView(icon, new LinearLayout.LayoutParams(dp(19), dp(19)));
            navIcons[i] = icon;

            // 文字——不设单行强裁切，给足高度让中文字符完整显示
            TextView label = new TextView(this);
            label.setText(tabLabels[i]);
            label.setTextSize(11);
            label.setIncludeFontPadding(true);
            label.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            label.setTextColor(active ? accentColor : mutedColor);
            label.setPadding(0, dp(4), 0, 0);
            item.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            navLabels[i] = label;

            // 选中指示器（小圆点）
            View dot = new View(this);
            int dotSize = dp(4);
            dot.setBackgroundResource(android.R.color.transparent);
            if (active) {
                dot.setBackgroundColor(accentColor);
            }
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotLp.topMargin = dp(4);
            dot.setLayoutParams(dotLp);
            item.addView(dot);
            navDots[i] = dot;

            navItems[i] = item;
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nav.addView(item, itemParams);
        }
        setContentView(root);
    }

    private FrameLayout.LayoutParams matchParams() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void switchTab(int index) {
        if (index == currentTab) {
            refreshCurrentTab();
            return;
        }
        int prevTab = currentTab;
        currentTab = index;

        animateTabItem(prevTab, false);
        animateTabItem(index, true);

        refreshCurrentTab();
    }

    /** 单个 tab 项在选中/取消选中之间的过渡动画：图标/文字变色 + 圆点指示器淡入淡出 + 文字粗细切换 */
    private void animateTabItem(int i, boolean toActive) {
        TabIconView icon = navIcons[i];
        TextView label = navLabels[i];
        View dot = navDots[i];

        if (itemAnimators[i] != null) itemAnimators[i].cancel();

        int accentColor = UiKit.getColorByName(this, "accent");
        int mutedColor = UiKit.getColorByName(this, "muted_light");
        float fromDotAlpha = dot.getAlpha();
        float toDotAlpha = toActive ? 1f : 0f;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(250);
        anim.setInterpolator(new DecelerateInterpolator(2f));
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            icon.setColor(blendColor(mutedColor, accentColor, toActive ? f : 1f - f));
            label.setTextColor(blendColor(mutedColor, accentColor, toActive ? f : 1f - f));
            label.setTypeface(toActive ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            dot.setAlpha(fromDotAlpha + (toDotAlpha - fromDotAlpha) * f);
            if (f > 0.5f && toActive) {
                dot.setBackgroundColor(accentColor);
            } else if (f <= 0.5f && !toActive) {
                dot.setBackgroundResource(android.R.color.transparent);
            }
        });
        anim.start();
        itemAnimators[i] = anim;
    }

    private int blendColor(int from, int to, float ratio) {
        float inv = 1f - ratio;
        int a = (int) (android.graphics.Color.alpha(from) * inv + android.graphics.Color.alpha(to) * ratio);
        int r = (int) (android.graphics.Color.red(from) * inv + android.graphics.Color.red(to) * ratio);
        int g = (int) (android.graphics.Color.green(from) * inv + android.graphics.Color.green(to) * ratio);
        int b = (int) (android.graphics.Color.blue(from) * inv + android.graphics.Color.blue(to) * ratio);
        return android.graphics.Color.argb(a, r, g, b);
    }

    // ═══════════════════════════════════════════
    //  Tab 内容调度
    // ═══════════════════════════════════════════

    private void showFeed() {
        content.removeAllViews();
        content.addView(new MemoryFeedPage(this, repository, this).build());
    }

    private void showTimeline() {
        content.removeAllViews();
        content.addView(new TimelinePage(this, repository, this).build());
    }

    private void showTodoTab() {
        content.removeAllViews();
        TodoListPage todoListPage = new TodoListPage(this, todoRepository, this, null, this::openTodoDetail);
        todoListPage.setRebuildCallback(this::refreshCurrentTab);
        content.addView(todoListPage.build());
    }

    @Override
    public void refreshCurrentTab() {
        if (overlayShowing) {
            renderOverlay();
            return;
        }
        if (currentTab == 0) showFeed();
        else if (currentTab == 1) showTimeline();
        else showTodoTab();
    }

    // ═══════════════════════════════════════════
    //  次级页面 overlay —— 日历 / 我的 / 百年
    // ═══════════════════════════════════════════

    private boolean overlayShowing = false;
    private int overlayKind = 0; // 0 = 无, 1 = 日历, 2 = 我的, 3 = 百年, 5 = 计划详情
    private String overlayTodoPlanId; // overlayKind == 5 时，当前查看的计划 id

    @Override
    public void openCalendar() {
        overlayKind = 1;
        overlayShowing = true;
        navWrapper.setVisibility(View.GONE);
        renderOverlay();
    }

    @Override
    public void openProfile() {
        overlayKind = 2;
        overlayShowing = true;
        navWrapper.setVisibility(View.GONE);
        renderOverlay();
    }

    @Override
    public void openCentury() {
        overlayKind = 3;
        overlayShowing = true;
        navWrapper.setVisibility(View.GONE);
        renderOverlay();
    }

    @Override
    public void openTodoList() {
        // 「计划」已是底部常驻 tab，直接切换过去即可，不再作为次级页面 overlay
        closeOverlay();
        switchTab(2);
    }

    @Override
    public void openTodoDetail(String planId) {
        overlayKind = 5;
        overlayTodoPlanId = planId;
        overlayShowing = true;
        navWrapper.setVisibility(View.GONE);
        renderOverlay();
    }

    @Override
    public void pickAvatarImage(Consumer<String> onPicked) {
        pendingAvatarCallback = onPicked;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_AVATAR_REQUEST);
    }

    private void closeOverlay() {
        if (!overlayShowing) return;
        overlayShowing = false;
        overlayKind = 0;
        overlay.removeAllViews();
        overlay.setVisibility(View.GONE);
        navWrapper.setVisibility(View.VISIBLE);
        // 次级页面里可能改动了会影响底层 tab 展示的状态（例如更换了头像），
        // 关闭时顺带刷新一下当前 tab，保证首页头像等信息保持同步
        refreshCurrentTab();
    }

    private void renderOverlay() {
        overlay.removeAllViews();
        overlay.setVisibility(View.VISIBLE);
        if (overlayKind == 1) {
            CalendarPage calendarPage = new CalendarPage(this, repository, this, this::closeOverlay);
            calendarPage.setRebuildCallback(this::renderOverlay);
            overlay.addView(calendarPage.build());
        } else if (overlayKind == 2) {
            ProfilePage profilePage = new ProfilePage(this, repository, this, this::closeOverlay);
            profilePage.setRebuildCallback(this::renderOverlay);
            overlay.addView(profilePage.build());
        } else if (overlayKind == 3) {
            overlay.addView(new CenturyPage(this, repository, this, this::closeOverlay).build());
        } else if (overlayKind == 5) {
            // 计划详情：从「计划」tab 进入，返回时关闭 overlay、回到 tab 列表即可
            TodoDetailPage todoDetailPage = new TodoDetailPage(this, todoRepository, this,
                    overlayTodoPlanId, this::closeOverlay);
            todoDetailPage.setRebuildCallback(this::renderOverlay);
            overlay.addView(todoDetailPage.build());
        }
    }

    // ═══════════════════════════════════════════
    //  PageHost 实现 —— 供各 Page 与 Activity 交互
    // ═══════════════════════════════════════════

    @Override
    public void showRecordDialog(LocalDate date, MemoryRecord editRecord) {
        recordEditDialog.show(date, editRecord);
    }

    @Override
    public void setCalendarSelection(YearMonth month, LocalDate date) {
        visibleMonth = month;
        selectedDate = date;
    }

    @Override
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    @Override
    public void setSelectedDate(LocalDate date) {
        selectedDate = date;
    }

    @Override
    public YearMonth getVisibleMonth() {
        return visibleMonth;
    }

    @Override
    public void setVisibleMonth(YearMonth month) {
        visibleMonth = month;
    }

    // ═══════════════════════════════════════════
    //  图片选择结果回调
    // ═══════════════════════════════════════════

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RecordEditDialog.PICK_IMAGE_REQUEST) {
            recordEditDialog.handleImagePickResult(resultCode, data);
        } else if (requestCode == PICK_AVATAR_REQUEST) {
            Consumer<String> callback = pendingAvatarCallback;
            pendingAvatarCallback = null;
            if (callback == null) return;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                android.net.Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {}
                callback.accept(uri.toString());
            } else {
                callback.accept("");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (overlayShowing) {
            closeOverlay();
            return;
        }
        super.onBackPressed();
    }

    private int dp(int v) {
        return UiKit.dp(this, v);
    }
}
