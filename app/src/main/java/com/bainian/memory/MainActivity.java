package com.bainian.memory;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE_REQUEST = 701;
    private static final DateTimeFormatter DATE_KEY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final List<MemoryRecord> records = new ArrayList<>();
    private final String[] tabLabels = {"日历", "时间线", "百年", "我的"};

    private SharedPreferences prefs;
    private LinearLayout root;
    private FrameLayout content;
    private LinearLayout nav;
    private LinearLayout[] navItems;
    private TabIconView[] navIcons;
    private TextView[] navLabels;
    private View navIndicator;
    private ValueAnimator indicatorAnimator;
    private LocalDate selectedDate = LocalDate.now();
    private YearMonth visibleMonth = YearMonth.now();
    private int currentTab = 0;
    private final List<Uri> pendingImageUris = new ArrayList<>();
    private LinearLayout imageStripRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("bainian_memory", MODE_PRIVATE);
        loadRecords();
        buildShell();
        showCalendar();
    }

    // ═══════════════════════════════════════════
    //  Shell — 整体框架 + 底部导航栏
    // ═══════════════════════════════════════════

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColorByName("bg"));

        // 状态栏安全区：让内容顶部与状态栏之间留有舒适间距
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarH = insets.getSystemWindowInsetTop();
            root.setPadding(0, statusBarH, 0, 0);
            return insets;
        });

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 导航栏顶部分隔线
        View divider = new View(this);
        divider.setBackgroundColor(getColorByName("line"));
        root.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        // 底部导航栏 —— 浮动胶囊设计
        FrameLayout navWrapper = new FrameLayout(this);
        navWrapper.setPadding(dp(16), 0, dp(16), dp(12));
        navWrapper.setBackgroundColor(getColorByName("bg")); // 与主背景融合
        root.addView(navWrapper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));

        // 胶囊容器
        FrameLayout capsule = new FrameLayout(this);
        capsule.setBackground(getDrawableByName("card_elevated"));
        capsule.setClipToOutline(true);
        capsule.setOutlineProvider(android.view.ViewOutlineProvider.BOUNDS);
        capsule.setClipToOutline(true);
        float navRadius = dp(28);
        capsule.setElevation(dp(6));
        capsule.setTranslationZ(dp(2));
        capsule.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), navRadius);
            }
        });
        // 初始设置一个最小圆角。宽度变化后由 switchTab 更新 outline。
        navWrapper.addView(capsule, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4), dp(6), dp(4), dp(4));
        capsule.addView(nav, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 底部滑动指示器
        navIndicator = new View(this);
        navIndicator.setBackground(createPillDrawable(getColorByName("accent")));
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(24), dp(4));
        ip.gravity = Gravity.BOTTOM | Gravity.START;
        ip.bottomMargin = dp(4);
        capsule.addView(navIndicator, ip);

        navItems = new LinearLayout[tabLabels.length];
        navIcons = new TabIconView[tabLabels.length];
        navLabels = new TextView[tabLabels.length];

        for (int i = 0; i < tabLabels.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(8), dp(4), dp(8), dp(4));
            item.setOnClickListener(v -> switchTab(index));

            TabIconView icon = new TabIconView(this);
            icon.setKind(i);
            icon.setSize(dp(24));
            icon.setColor(i == 0 ? getColorByName("accent") : getColorByName("muted"));
            item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
            navIcons[i] = icon;

            TextView label = new TextView(this);
            label.setText(tabLabels[i]);
            label.setTextSize(11);
            label.setTypeface(i == 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            label.setTextColor(i == 0 ? getColorByName("accent") : getColorByName("muted"));
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, dp(4), 0, 0);
            item.addView(label);
            navLabels[i] = label;

            navItems[i] = item;
            nav.addView(item, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        setContentView(root);
    }

    private void switchTab(int index) {
        if (indicatorAnimator != null) indicatorAnimator.cancel();
        int prevTab = currentTab;
        currentTab = index;

        // 图标和文字颜色过渡
        for (int i = 0; i < navIcons.length; i++) {
            boolean active = i == index;
            navIcons[i].setColor(active ? getColorByName("accent") : getColorByName("muted"));
            navLabels[i].setTextColor(active ? getColorByName("accent") : getColorByName("muted"));
            navLabels[i].setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }

        // 滑动指示器动画
        nav.post(() -> {
            LinearLayout fromItem = navItems[prevTab];
            LinearLayout toItem = navItems[index];
            float fromX = fromItem.getX() + (fromItem.getWidth() - dp(24)) / 2f;
            float toX = toItem.getX() + (toItem.getWidth() - dp(24)) / 2f;

            ValueAnimator anim = ValueAnimator.ofFloat(fromX, toX);
            anim.setDuration(280);
            anim.setInterpolator(new DecelerateInterpolator(2f));
            anim.addUpdateListener(a -> {
                float val = (float) a.getAnimatedValue();
                navIndicator.setTranslationX(val);
            });
            anim.start();
            indicatorAnimator = anim;
        });

        if (index == 0) showCalendar();
        if (index == 1) showTimeline();
        if (index == 2) showCentury();
        if (index == 3) showProfile();
    }

    private android.graphics.drawable.Drawable createPillDrawable(int color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(2));
        gd.setColor(color);
        return gd;
    }

    // ═══════════════════════════════════════════
    //  Tab 0 — 日历
    // ═══════════════════════════════════════════

    private void showCalendar() {
        content.removeAllViews();
        FrameLayout page = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(20), dp(20), dp(100));
        scroll.addView(body);
        page.addView(scroll, match());

        // 月份标题
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(14));
        TextView title = new TextView(this);
        title.setText(visibleMonth.getYear() + "年" + visibleMonth.getMonthValue() + "月");
        title.setTextColor(getColorByName("ink"));
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(navButton("◀", v -> { visibleMonth = visibleMonth.minusMonths(1); showCalendar(); }));
        header.addView(navButton("今天", v -> { selectedDate = LocalDate.now(); visibleMonth = YearMonth.now(); showCalendar(); }));
        header.addView(navButton("▶", v -> { visibleMonth = visibleMonth.plusMonths(1); showCalendar(); }));
        body.addView(header);

        // 日历网格
        LinearLayout calWrap = new LinearLayout(this);
        calWrap.setOrientation(LinearLayout.VERTICAL);
        calWrap.setPadding(dp(4), dp(14), dp(4), dp(14));
        calWrap.setBackground(getDrawableByName("card_elevated"));

        GridLayout weekBar = new GridLayout(this);
        weekBar.setColumnCount(7);
        for (String w : new String[]{"一", "二", "三", "四", "五", "六", "日"}) {
            TextView wl = new TextView(this);
            wl.setText(w); wl.setTextSize(12); wl.setTextColor(getColorByName("muted"));
            wl.setGravity(Gravity.CENTER); wl.setPadding(0, 0, 0, dp(6));
            weekBar.addView(wl, gridCellParam());
        }
        calWrap.addView(weekBar);

        GridLayout dayGrid = new GridLayout(this);
        dayGrid.setColumnCount(7);
        int leading = visibleMonth.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leading; i++) dayGrid.addView(new Space(this), gridCellParam());
        for (int d = 1; d <= visibleMonth.lengthOfMonth(); d++) {
            dayGrid.addView(dayCell(visibleMonth.atDay(d)), gridCellParam());
        }
        calWrap.addView(dayGrid);
        body.addView(calWrap, fullWidthMargin(0, dp(10)));

        // 日期标题 + 记录
        TextView dayH = new TextView(this);
        dayH.setText(selectedDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)));
        dayH.setTextColor(getColorByName("ink")); dayH.setTextSize(17);
        dayH.setTypeface(Typeface.DEFAULT_BOLD); dayH.setPadding(0, dp(14), 0, dp(10));
        body.addView(dayH);
        renderDayRecords(body, selectedDate);

        // FAB
        TextView fab = new TextView(this);
        fab.setText("+"); fab.setTextColor(Color.WHITE); fab.setTextSize(28);
        fab.setGravity(Gravity.CENTER); fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setBackground(getDrawableByName("fab_elevated"));
        fab.setOnClickListener(v -> showRecordDialog(selectedDate, null));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(56), dp(56));
        fp.gravity = Gravity.BOTTOM | Gravity.END; fp.setMargins(0, 0, dp(24), dp(24));
        page.addView(fab, fp);

        content.addView(page, match());
    }

    private View dayCell(LocalDate date) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(2), dp(4), dp(2), dp(4));
        boolean today = date.equals(LocalDate.now());
        boolean selected = date.equals(selectedDate);

        TextView num = new TextView(this);
        num.setText(String.valueOf(date.getDayOfMonth()));
        num.setTextSize(14);
        num.setGravity(Gravity.CENTER);

        if (today) {
            num.setTextColor(Color.WHITE);
            num.setBackground(getDrawableByName("calendar_today_bg"));
            num.setWidth(dp(34)); num.setHeight(dp(34));
            num.setGravity(Gravity.CENTER);
            num.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (selected) {
            num.setTextColor(getColorByName("accent"));
            num.setBackground(getDrawableByName("calendar_selected_bg"));
            num.setWidth(dp(34)); num.setHeight(dp(34));
            num.setGravity(Gravity.CENTER);
            num.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            num.setTextColor(getColorByName("ink"));
        }
        cell.addView(num);

        int count = recordsFor(date).size();
        if (count > 0) {
            View dot = new View(this);
            dot.setBackground(getDrawableByName("day_dot"));
            cell.addView(dot, new LinearLayout.LayoutParams(dp(5), dp(5)));
        } else {
            cell.addView(new Space(this), new LinearLayout.LayoutParams(dp(5), dp(5)));
        }

        cell.setOnClickListener(v -> { selectedDate = date; showCalendar(); });
        return cell;
    }

    private void renderDayRecords(LinearLayout parent, LocalDate date) {
        List<MemoryRecord> dayRecords = recordsFor(date);
        if (dayRecords.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("今天还没有记录。按右下角「+」，写下一件值得留下的事。");
            empty.setTextColor(getColorByName("muted")); empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER); empty.setPadding(dp(28), dp(32), dp(28), dp(32));
            empty.setBackground(getDrawableByName("card_elevated"));
            empty.setLineSpacing(dp(6), 1f);
            parent.addView(empty);
            return;
        }
        for (MemoryRecord r : dayRecords) parent.addView(recordCard(r, true));
    }

    // ═══════════════════════════════════════════
    //  Tab 1 — 时间线
    // ═══════════════════════════════════════════

    private void showTimeline() {
        content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = pageBody();
        scroll.addView(body);

        body.addView(pageTitle("时间线"));
        body.addView(pageDesc("按时间回看所有记忆，适合寻找人生阶段里的细节。"));

        List<MemoryRecord> sorted = new ArrayList<>(records);
        Collections.sort(sorted, (a, b) -> b.date.compareTo(a.date));
        if (sorted.isEmpty()) {
            body.addView(emptyState("还没有任何记录。先去日历里写下第一件事。"));
        } else {
            String lastMonth = "";
            for (MemoryRecord r : sorted) {
                String month = r.date.substring(0, 7);
                if (!month.equals(lastMonth)) {
                    TextView mt = new TextView(this);
                    mt.setText(month.replace("-", "年") + "月");
                    mt.setTextColor(getColorByName("ink")); mt.setTextSize(18);
                    mt.setTypeface(Typeface.DEFAULT_BOLD);
                    mt.setPadding(0, dp(20), 0, dp(10));
                    body.addView(mt);
                    lastMonth = month;
                }
                body.addView(recordCard(r, false));
            }
        }
        content.addView(scroll, match());
    }

    // ═══════════════════════════════════════════
    //  Tab 2 — 百年
    // ═══════════════════════════════════════════

    private void showCentury() {
        content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = pageBody();
        body.setPadding(dp(20), dp(20), dp(20), dp(32));
        scroll.addView(body);

        int birthYear = prefs.getInt("birth_year", LocalDate.now().getYear() - 30);
        int currentYear = LocalDate.now().getYear();
        int age = Math.min(Math.max(0, currentYear - birthYear), 100);
        int livedDays = countRecordedDays();
        int recordedYears = 0;
        for (int i = 0; i < 100; i++) {
            if (recordCountForYear(birthYear + i) > 0) recordedYears++;
        }

        // ── 生命进度卡片 ──
        LinearLayout card1 = new LinearLayout(this);
        card1.setOrientation(LinearLayout.VERTICAL);
        card1.setPadding(dp(20), dp(20), dp(20), dp(16));
        card1.setBackground(getDrawableByName("card_elevated"));

        TextView card1Title = new TextView(this);
        card1Title.setText("生命进度");
        card1Title.setTextColor(getColorByName("ink"));
        card1Title.setTextSize(16);
        card1Title.setTypeface(Typeface.DEFAULT_BOLD);
        card1Title.setPadding(0, 0, 0, dp(12));
        card1.addView(card1Title);

        // 进度条
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(getDrawableByName("progress_track"));
        track.setClipToOutline(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(11));
        tp.setMargins(0, 0, 0, dp(10));
        track.setLayoutParams(tp);

        View fill = new View(this);
        fill.setBackgroundColor(getColorByName("accent"));
        int fw = Math.max(age, 1);
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, fw));
        if (age < 100) {
            track.addView(new View(this), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - fw));
        }
        card1.addView(track);

        // 年龄 + 统计同行
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView ageText = new TextView(this);
        ageText.setText(age + "/100");
        ageText.setTextColor(getColorByName("accent"));
        ageText.setTextSize(18);
        ageText.setTypeface(Typeface.DEFAULT_BOLD);
        infoRow.addView(ageText);

        View spacer = new View(this);
        infoRow.addView(spacer, new LinearLayout.LayoutParams(dp(14), 0));

        TextView statsText = new TextView(this);
        statsText.setText("已记录 " + recordedYears + " 个年份 · 点亮 " + livedDays + " 天");
        statsText.setTextColor(getColorByName("muted"));
        statsText.setTextSize(12);
        infoRow.addView(statsText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        card1.addView(infoRow);
        body.addView(card1, fullWidthMargin(0, dp(16)));

        // ── 百年棋盘 ──
        LinearLayout card2 = new LinearLayout(this);
        card2.setOrientation(LinearLayout.VERTICAL);
        card2.setPadding(dp(14), dp(16), dp(14), dp(14));
        card2.setBackground(getDrawableByName("card_elevated"));

        for (int dec = 0; dec < 10; dec++) {
            int decStart = birthYear + dec * 10;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            TextView dl = new TextView(this);
            dl.setText(decStart + "s");
            dl.setTextColor(getColorByName("muted"));
            dl.setTextSize(10);
            dl.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            dl.setPadding(0, 0, dp(6), 0);
            dl.setSingleLine(true);
            row.addView(dl, new LinearLayout.LayoutParams(dp(42), dp(24)));

            for (int j = 0; j < 10; j++) {
                row.addView(yearCell(decStart + j, currentYear));
            }

            card2.addView(row);
        }

        // 内联图例
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, dp(10), 0, 0);
        legend.addView(legItem(getDrawableByName("decade_cell_filled"), "有记录"));
        legend.addView(legItem(getDrawableByName("decade_cell_current"), "今年"));
        legend.addView(legItem(getDrawableByName("decade_cell_empty"), "空白"));
        legend.addView(legItem(getDrawableByName("decade_cell_future"), "未来"));
        card2.addView(legend);

        body.addView(card2, fullWidthMargin(0, 0));

        content.addView(scroll, match());
    }

    private View yearCell(int year, int currentYear) {
        int count = recordCountForYear(year);
        boolean hasRec = count > 0;
        boolean isFut = year > currentYear;
        boolean isCurrent = year == currentYear;

        TextView tv = new TextView(this);
        tv.setText(String.valueOf(year % 100));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(10);

        if (hasRec) {
            tv.setBackground(getDrawableByName("decade_cell_filled"));
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (isCurrent) {
            tv.setBackground(getDrawableByName("decade_cell_current"));
            tv.setTextColor(getColorByName("accent"));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (isFut) {
            tv.setBackground(getDrawableByName("decade_cell_future"));
            tv.setTextColor(getColorByName("muted_light"));
            tv.setAlpha(0.4f);
        } else {
            tv.setBackground(getDrawableByName("decade_cell_empty"));
            tv.setTextColor(getColorByName("muted"));
        }

        tv.setOnClickListener(v -> {
            visibleMonth = YearMonth.of(year, 1);
            selectedDate = visibleMonth.atDay(1);
            switchTab(0);
        });

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(24), 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        tv.setLayoutParams(p);
        return tv;
    }

    private View legItem(android.graphics.drawable.Drawable bg, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(0, 0, dp(12), 0);

        View swatch = new View(this);
        swatch.setBackground(bg);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(8), dp(8));
        sp.setMargins(0, 0, dp(4), 0);
        swatch.setLayoutParams(sp);
        item.addView(swatch);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(getColorByName("muted"));
        tv.setTextSize(10);
        item.addView(tv);

        return item;
    }

    // ═══════════════════════════════════════════
    //  Tab 3 — 我的
    // ═══════════════════════════════════════════

    private void showProfile() {
        content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout body = pageBody();
        body.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(body);

        // ── 头部卡片：头像 + 信息 ──
        LinearLayout headerCard = new LinearLayout(this);
        headerCard.setOrientation(LinearLayout.VERTICAL);
        headerCard.setGravity(Gravity.CENTER);
        headerCard.setPadding(dp(28), dp(32), dp(28), dp(28));
        headerCard.setBackground(getDrawableByName("card_elevated"));
        headerCard.setLayoutParams(fullWidthMargin(0, dp(20)));

        // 圆形头像
        ProfileAvatarView avatar = new ProfileAvatarView(this);
        avatar.setSizeAndColor(dp(72), getColorByName("accent"));
        headerCard.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(72)));

        // 用户名
        TextView name = new TextView(this);
        name.setText("生命记录者");
        name.setTextColor(getColorByName("ink"));
        name.setTextSize(20);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setPadding(0, dp(14), 0, dp(6));
        headerCard.addView(name);

        // 统计行
        int birthYear = prefs.getInt("birth_year", LocalDate.now().getYear() - 30);
        int age = Math.min(Math.max(0, LocalDate.now().getYear() - birthYear), 100);
        int totalDays = countRecordedDays();
        int totalRecords = records.size();
        int recordedYears = 0;
        for (int i = 0; i < 100; i++) {
            if (recordCountForYear(birthYear + i) > 0) recordedYears++;
        }

        TextView stat = new TextView(this);
        stat.setText("年龄 " + age + " 岁  ·  已记录 " + totalDays + " 天  ·  " + totalRecords + " 条记忆");
        stat.setTextColor(getColorByName("muted"));
        stat.setTextSize(13);
        stat.setGravity(Gravity.CENTER);
        headerCard.addView(stat);

        body.addView(headerCard);

        // ── 统计卡片行 ──
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setLayoutParams(fullWidthMargin(0, dp(20)));
        body.addView(statsRow);

        statsRow.addView(statCard("记录天数", String.valueOf(totalDays), "天", getColorByName("accent")));
        statsRow.addView(statSpacer());
        statsRow.addView(statCard("记忆条数", String.valueOf(totalRecords), "条", 0xFFE8A838));
        statsRow.addView(statSpacer());
        statsRow.addView(statCard("活跃年份", String.valueOf(recordedYears), "年", 0xFF5B9BD5));

        // ── 个人信息分组 ──
        body.addView(sectionHeader("个人信息"));
        body.addView(settingsItem(SettingsIconKind.BIRTH, "设置出生年份", v -> editBirthYear()));

        // ── 关于分组 ──
        body.addView(sectionHeader("关于"));
        body.addView(settingsItem(SettingsIconKind.ABOUT, "关于百年", v -> new AlertDialog.Builder(this)
                .setTitle("百年")
                .setMessage("记录你认真生活过的每一天。\n\n当前版本：1.0 MVP\n\n把每天的小事，放进一生的日历里。")
                .setPositiveButton("知道了", null).show()));
        body.addView(settingsItem(SettingsIconKind.SYSTEM, "系统应用设置", v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));

        // ── 底部版本号 ──
        Space sp = new Space(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        body.addView(sp);

        TextView version = new TextView(this);
        version.setText("百年 · v1.0 MVP");
        version.setTextColor(getColorByName("muted"));
        version.setTextSize(12);
        version.setGravity(Gravity.CENTER);
        body.addView(version, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(scroll, match());
    }

    // ── 统计小卡片 ──
    private View statCard(String label, String value, String unit, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(18), dp(12), dp(18));
        card.setBackground(getDrawableByName("card_elevated"));

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(accentColor);
        val.setTextSize(28);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setGravity(Gravity.CENTER);
        card.addView(val);

        TextView unitTv = new TextView(this);
        unitTv.setText(unit);
        unitTv.setTextColor(accentColor);
        unitTv.setTextSize(10);
        unitTv.setPadding(0, dp(2), 0, dp(4));
        unitTv.setGravity(Gravity.CENTER);
        card.addView(unitTv);

        TextView lb = new TextView(this);
        lb.setText(label);
        lb.setTextColor(getColorByName("muted"));
        lb.setTextSize(11);
        lb.setGravity(Gravity.CENTER);
        card.addView(lb);

        card.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private View statSpacer() {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(10),
                ViewGroup.LayoutParams.MATCH_PARENT));
        return s;
    }

    // ── 分组标题 ──
    private TextView sectionHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(getColorByName("ink"));
        tv.setTextSize(15);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(8), 0, dp(12));
        tv.setLayoutParams(fullWidthMargin(0, 0));
        return tv;
    }

    // ── 设置项图标类型 ──
    enum SettingsIconKind { BIRTH, ABOUT, SYSTEM }

    private View settingsItem(SettingsIconKind kind, String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(16), dp(18), dp(16));
        row.setBackground(getDrawableByName("card_elevated"));
        row.setOnClickListener(listener);

        // 自定义图标
        SettingsIconView ic = new SettingsIconView(this);
        ic.setKind(kind);
        ic.setIconColor(getColorByName("accent"));
        int iconSize = dp(22);
        ic.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        row.addView(ic);
        // 间距
        Space gap = new Space(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(dp(14), 0));
        row.addView(gap);

        TextView lb = new TextView(this);
        lb.setText(label); lb.setTextColor(getColorByName("ink")); lb.setTextSize(15);
        row.addView(lb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView ar = new TextView(this);
        ar.setText("›"); ar.setTextColor(getColorByName("muted")); ar.setTextSize(20);
        row.addView(ar);

        row.setLayoutParams(fullWidthMargin(0, dp(10)));
        return row;
    }

    // ── 头部头像 View ──
    private static class ProfileAvatarView extends View {
        private float avatarSize;
        private int avatarColor;
        private Paint fgPaint, bgPaint;

        public ProfileAvatarView(Context context) {
            super(context);
            fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fgPaint.setStyle(Paint.Style.STROKE);
            fgPaint.setStrokeCap(Paint.Cap.ROUND);
            fgPaint.setStrokeJoin(Paint.Join.ROUND);
            fgPaint.setColor(Color.WHITE);
            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);
        }

        public void setSizeAndColor(float size, int color) {
            avatarSize = size;
            avatarColor = color;
            bgPaint.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float r = Math.min(getWidth(), getHeight()) / 2f;

            // 背景圆
            canvas.drawCircle(cx, cy, r, bgPaint);

            // 人物图标线宽
            float sw = r * 0.08f;
            fgPaint.setStrokeWidth(sw);

            // 头部
            float headR = r * 0.26f;
            float headY = cy - r * 0.28f;
            canvas.drawCircle(cx, headY, headR, fgPaint);

            // 身体
            float bodyTop = headY + headR + sw * 2;
            Path path = new Path();
            path.moveTo(cx - r * 0.55f, cy + r * 0.6f);
            path.quadTo(cx - r * 0.45f, bodyTop, cx, bodyTop);
            path.quadTo(cx + r * 0.45f, bodyTop, cx + r * 0.55f, cy + r * 0.6f);
            canvas.drawPath(path, fgPaint);
        }
    }

    // ── 设置项图标 View ──
    private static class SettingsIconView extends View {
        private SettingsIconKind kind = SettingsIconKind.BIRTH;
        private int iconColor = 0xFF5F6368;
        private Paint paint;
        private Path path;

        public SettingsIconView(Context context) {
            super(context);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            path = new Path();
        }

        public void setKind(SettingsIconKind k) { kind = k; invalidate(); }
        public void setIconColor(int c) { iconColor = c; paint.setColor(c); invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            float m = w * 0.18f;
            paint.setColor(iconColor);
            path.reset();

            switch (kind) {
                case BIRTH: // 蛋糕
                    paint.setStrokeWidth(w * 0.085f);
                    float cakeTop = h * 0.28f;
                    float cakeBot = h * 0.72f;
                    // 蜡烛
                    float cMx = w / 2f;
                    canvas.drawLine(cMx, h * 0.12f, cMx, cakeTop + w * 0.02f, paint);
                    // 火焰
                    paint.setStyle(Paint.Style.FILL);
                    float flameY = h * 0.08f;
                    path.moveTo(cMx - w * 0.1f, flameY + w * 0.08f);
                    path.quadTo(cMx - w * 0.06f, flameY, cMx, flameY);
                    path.quadTo(cMx + w * 0.06f, flameY, cMx + w * 0.1f, flameY + w * 0.08f);
                    path.quadTo(cMx + w * 0.04f, flameY + w * 0.05f, cMx, flameY + w * 0.1f);
                    path.quadTo(cMx - w * 0.04f, flameY + w * 0.05f, cMx - w * 0.1f, flameY + w * 0.08f);
                    path.close();
                    canvas.drawPath(path, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    // 蛋糕体
                    canvas.drawRoundRect(m, cakeTop, w - m, cakeBot, w * 0.06f, w * 0.06f, paint);
                    // 中间横线
                    canvas.drawLine(m + w * 0.1f, cakeTop + (cakeBot - cakeTop) * 0.4f,
                            w - m - w * 0.1f, cakeTop + (cakeBot - cakeTop) * 0.4f, paint);
                    break;

                case ABOUT: // 信息圆圈
                    paint.setStrokeWidth(w * 0.09f);
                    float cr = w * 0.33f;
                    canvas.drawCircle(w / 2f, h / 2f, cr, paint);
                    // 竖线
                    canvas.drawLine(w / 2f, h / 2f - cr * 0.45f,
                            w / 2f, h / 2f - cr * 0.2f, paint);
                    // 小点
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(w / 2f, h / 2f + cr * 0.3f, w * 0.06f, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    break;

                case SYSTEM: // 齿轮
                    paint.setStrokeWidth(w * 0.07f);
                    float gr = w * 0.28f;
                    float gcx = w / 2f, gcy = h / 2f;
                    canvas.drawCircle(gcx, gcy, gr, paint);
                    // 锯齿
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.toRadians(i * 60);
                        float ix = (float)(gcx + (gr + w * 0.06f) * Math.cos(angle));
                        float iy = (float)(gcy + (gr + w * 0.06f) * Math.sin(angle));
                        float ox = (float)(gcx + (gr - w * 0.10f) * Math.cos(angle));
                        float oy = (float)(gcy + (gr - w * 0.10f) * Math.sin(angle));
                        canvas.drawLine(ix, iy, ox, oy, paint);
                    }
                    // 中心小圆
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(gcx, gcy, w * 0.08f, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    break;
            }
        }
    }

    // ═══════════════════════════════════════════
    //  记录卡片（共用）
    // ═══════════════════════════════════════════

    private View recordCard(MemoryRecord r, boolean allowDelete) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(getDrawableByName("card_elevated"));
        card.setLayoutParams(fullWidthMargin(dp(4), dp(12)));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView ti = new TextView(this);
        ti.setText(r.title); ti.setTextColor(getColorByName("ink"));
        ti.setTextSize(17); ti.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(ti, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (allowDelete) {
            TextView del = new TextView(this);
            del.setText("✕"); del.setTextSize(16); del.setTextColor(getColorByName("muted"));
            del.setPadding(dp(14), dp(4), dp(4), dp(4));
            del.setOnClickListener(v -> confirmDelete(r));
            titleRow.addView(del);
        }
        card.addView(titleRow);

        TextView dt = new TextView(this);
        dt.setText(r.date); dt.setTextColor(getColorByName("muted"));
        dt.setTextSize(12); dt.setPadding(0, dp(4), 0, dp(8));
        card.addView(dt);

        if (!r.body.isEmpty()) {
            TextView body = new TextView(this);
            body.setText(r.body); body.setTextColor(getColorByName("ink_soft"));
            body.setTextSize(15); body.setLineSpacing(dp(5), 1f);
            body.setPadding(0, 0, 0, dp(10));
            card.addView(body);
        }

        List<String> imageUris = r.getImageUris();
        if (!imageUris.isEmpty()) {
            card.addView(buildImageGallery(imageUris));
        }

        if (!r.mood.isEmpty() || !r.tag.isEmpty()) {
            LinearLayout chips = new LinearLayout(this);
            chips.setGravity(Gravity.LEFT);
            if (!r.mood.isEmpty()) chips.addView(chip(r.mood));
            if (!r.tag.isEmpty()) chips.addView(chip(r.tag));
            card.addView(chips);
        }

        // 整卡点击编辑（删除按钮自身会消费点击事件，不冲突）
        card.setOnClickListener(v -> {
            try {
                showRecordDialog(LocalDate.parse(r.date, DATE_KEY), r);
            } catch (Exception e) {
                showRecordDialog(LocalDate.now(), r);
            }
        });
        return card;
    }

    private View buildImageGallery(List<String> uris) {
        int n = uris.size();
        int gap = dp(3);
        int radius = dp(8);
        android.graphics.drawable.Drawable bg = getDrawableByName("card_bg");

        if (n == 1) {
            // 单图：全宽 240dp
            FrameLayout wrap = new FrameLayout(this);
            wrap.setClipToOutline(true);
            wrap.setBackground(bg);
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Uri u0 = Uri.parse(uris.get(0));
            img.setImageURI(u0);
            img.setOnClickListener(v -> showImageViewer(u0));
            wrap.addView(img, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));
            wrap.setLayoutParams(fullWidthMargin(dp(2), 0));
            return wrap;
        }
        if (n == 2) {
            // 双图：左右并排，各 50%
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < 2; i++) {
                FrameLayout wrap = new FrameLayout(this);
                wrap.setClipToOutline(true);
                wrap.setBackground(bg);
                ImageView img = new ImageView(this);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Uri u = Uri.parse(uris.get(i));
                img.setImageURI(u);
                img.setOnClickListener(v -> showImageViewer(u));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(200), 1f);
                if (i == 0) lp.rightMargin = gap;
                wrap.addView(img, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                row.addView(wrap, lp);
            }
            return row;
        }
        if (n == 3) {
            // 三图：左大(66%) + 右二叠(stack)
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            // 左图
            FrameLayout left = new FrameLayout(this);
            left.setClipToOutline(true); left.setBackground(bg);
            ImageView li = new ImageView(this);
            li.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Uri lu = Uri.parse(uris.get(0));
            li.setImageURI(lu);
            li.setOnClickListener(v -> showImageViewer(lu));
            left.addView(li, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            row.addView(left, new LinearLayout.LayoutParams(0, dp(200), 0.66f));
            // 右侧间距
            View sp = new View(this);
            row.addView(sp, new LinearLayout.LayoutParams(gap, 0));
            // 右侧两张叠图
            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            for (int i = 1; i < 3; i++) {
                FrameLayout wrap = new FrameLayout(this);
                wrap.setClipToOutline(true); wrap.setBackground(bg);
                ImageView img = new ImageView(this);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Uri ru = Uri.parse(uris.get(i));
                img.setImageURI(ru);
                img.setOnClickListener(v -> showImageViewer(ru));
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                if (i == 2) rp.topMargin = gap;
                wrap.addView(img, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                right.addView(wrap, rp);
            }
            row.addView(right, new LinearLayout.LayoutParams(0, dp(200), 0.34f));
            return row;
        }
        // 4+ 图：2 列网格，显示前 4 或前 3 + "+N"
        int show = Math.min(n, 4);
        int cols = 2;
        int rows = (int) Math.ceil(show / 2.0);
        int cellH = dp(120);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            if (r > 0) row.setPadding(0, gap, 0, 0);
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                FrameLayout wrap = new FrameLayout(this);
                wrap.setClipToOutline(true);
                wrap.setBackground(bg);
                ImageView img = new ImageView(this);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (idx < uris.size()) {
                    Uri gu = Uri.parse(uris.get(idx));
                    img.setImageURI(gu);
                    img.setOnClickListener(v -> showImageViewer(gu));
                }
                wrap.addView(img, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                // "+N" overlay on last visible cell when there are more
                if (idx == show - 1 && n > show) {
                    TextView overlay = new TextView(this);
                    overlay.setText("+" + (n - show + 1));
                    overlay.setTextSize(28);
                    overlay.setTextColor(Color.WHITE);
                    overlay.setGravity(Gravity.CENTER);
                    overlay.setBackgroundColor(0x88000000);
                    wrap.addView(overlay, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, cellH, 1f);
                if (c == 0) cp.rightMargin = gap;
                wrap.setLayoutParams(cp);
                row.addView(wrap);
            }
            grid.addView(row);
        }
        return grid;
    }

    // ═══════════════════════════════════════════
    //  全屏图片查看
    // ═══════════════════════════════════════════

    private void showImageViewer(Uri imageUri) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        root.setOnClickListener(v -> dialog.dismiss());

        // Zoomable ImageView via custom ScaleGestureDetector
        ZoomImageView zoomView = new ZoomImageView(this);
        zoomView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        zoomView.setImageURI(imageUri);
        root.addView(zoomView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 关闭按钮
        TextView close = new TextView(this);
        close.setText("✕"); close.setTextColor(Color.WHITE);
        close.setTextSize(22); close.setGravity(Gravity.CENTER);
        close.setPadding(dp(16), dp(40), dp(16), dp(16));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.END | Gravity.TOP;
        root.addView(close, clp);
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(root);
        dialog.show();
    }

    /** 支持双指缩放的 ImageView */
    /** 底部导航自定义图标 — 简洁几何图形绘制 */
    private static class TabIconView extends View {
        private int kind;
        private int iconColor = 0xFF5F6368;
        private float iconSize;
        private Paint paint;
        private Path path;

        public TabIconView(Context context) {
            super(context);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            path = new Path();
        }

        public void setKind(int k) { kind = k; invalidate(); }
        public void setSize(float s) { iconSize = s; invalidate(); }
        public void setColor(int c) { iconColor = c; paint.setColor(c); invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            float m = w * 0.22f; // margin
            float s = iconSize > 0 ? iconSize : w;

            paint.setColor(iconColor);
            path.reset();

            switch (kind) {
                case 0: // 日历 —— 方形 + 内部横线
                    paint.setStrokeWidth(s * 0.07f);
                    // 外框
                    float r = s * 0.13f;
                    canvas.drawRoundRect(m, m, w - m, h - m, r, r, paint);
                    // 顶部横条
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(m, m, w - m, m + (h - 2 * m) * 0.38f, r, r, paint);
                    // 内部两列
                    paint.setStyle(Paint.Style.STROKE);
                    float cx = w / 2f;
                    float topY = m + (h - 2 * m) * 0.38f + (h - 2 * m) * 0.10f;
                    float botY = h - m - (h - 2 * m) * 0.05f;
                    canvas.drawLine(m * 2.3f, topY, m * 2.3f, botY, paint);
                    canvas.drawLine(w - m * 2.3f, topY, w - m * 2.3f, botY, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    break;

                case 1: // 时间线 —— 三根竖线
                    paint.setStrokeWidth(s * 0.09f);
                    for (int j = 0; j < 3; j++) {
                        float x = w * (0.25f + 0.25f * j);
                        float ty = j == 1 ? m + s * 0.08f : m + s * 0.02f;
                        float by = j == 1 ? h - m - s * 0.08f : h - m - s * 0.02f;
                        canvas.drawLine(x, ty, x, by, paint);
                    }
                    break;

                case 2: // 百年 —— 菱形 + 内点
                    paint.setStrokeWidth(s * 0.08f);
                    float cx2 = w / 2f, cy2 = h / 2f;
                    float hs = s * 0.28f;
                    path.moveTo(cx2, cy2 - hs);
                    path.lineTo(cx2 + hs, cy2);
                    path.lineTo(cx2, cy2 + hs);
                    path.lineTo(cx2 - hs, cy2);
                    path.close();
                    canvas.drawPath(path, paint);
                    // 内点
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx2, cy2, s * 0.06f, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    break;

                case 3: // 我的 —— 圆 + 半圆
                    paint.setStrokeWidth(s * 0.08f);
                    float cx3 = w / 2f;
                    float headR = s * 0.15f;
                    float headY = m + headR + s * 0.03f;
                    canvas.drawCircle(cx3, headY, headR, paint);
                    // 身体弧线
                    float bodyTop = headY + headR + s * 0.04f;
                    path.moveTo(cx3 - s * 0.2f, bodyTop + s * 0.28f);
                    path.cubicTo(cx3 - s * 0.2f, bodyTop, cx3 + s * 0.2f, bodyTop,
                            cx3 + s * 0.2f, bodyTop + s * 0.28f);
                    canvas.drawPath(path, paint);
                    break;
            }
        }
    }

    private static class ZoomImageView extends ImageView {
        private ScaleGestureDetector scaleDetector;
        private float scale = 1f;
        private static final float MIN_SCALE = 0.5f;
        private static final float MAX_SCALE = 5f;

        public ZoomImageView(Context context) {
            super(context);
            scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            canvas.scale(scale, scale, getWidth() / 2f, getHeight() / 2f);
            super.onDraw(canvas);
            canvas.restore();
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
                invalidate();
                return true;
            }
        }
    }

    // ═══════════════════════════════════════════
    //  记录弹窗
    // ═══════════════════════════════════════════

    private void showRecordDialog(LocalDate date, MemoryRecord editRecord) {
        pendingImageUris.clear();
        imageStripRow = null;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(16), dp(24), dp(8));

        EditText ti = styledInput("标题，例如：和朋友在湖边散步", false);
        EditText bo = styledInput("写下这件事的细节...", true);
        EditText mo = styledInput("心情，例如：平静、开心、疲惫", false);
        EditText ta = styledInput("标签，例如：家人、旅行、工作", false);

        form.addView(ti); form.addView(bo); form.addView(mo); form.addView(ta);

        // ── 照片预览条 ──
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setPadding(0, dp(4), 0, 0);
        imageStripRow = new LinearLayout(this);
        imageStripRow.setOrientation(LinearLayout.HORIZONTAL);
        imageStripRow.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(imageStripRow);
        form.addView(scroll);

        // 添加照片按钮
        Button pb = new Button(this);
        pb.setText("+ 添加照片"); pb.setTextSize(14); pb.setAllCaps(false);
        pb.setPadding(dp(16), dp(12), dp(16), dp(12));
        pb.setBackground(getDrawableByName("setting_item_bg"));
        pb.setTextColor(getColorByName("accent"));
        pb.setOnClickListener(v -> pickImage());
        form.addView(pb, fullWidthMargin(0, dp(8)));

        boolean isEdit = editRecord != null;
        String titleText = date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
                + (isEdit ? " · 编辑记录" : " · 新记录");

        // 编辑模式：预填充已有内容
        if (isEdit) {
            ti.setText(editRecord.title);
            bo.setText(editRecord.body);
            mo.setText(editRecord.mood);
            ta.setText(editRecord.tag);
            for (String s : editRecord.getImageUris()) {
                try { pendingImageUris.add(Uri.parse(s)); } catch (Exception ignored) {}
            }
        }
        refreshImageStrip();

        final MemoryRecord target = editRecord;
        new AlertDialog.Builder(this)
                .setTitle(titleText)
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, which) -> {
                    String mt = ti.getText().toString().trim();
                    String bt = bo.getText().toString().trim();
                    if (mt.isEmpty() && bt.isEmpty()) {
                        Toast.makeText(this, "至少写一点内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> uris = new ArrayList<>();
                    for (Uri u : pendingImageUris) uris.add(u.toString());
                    if (isEdit && target != null) {
                        // 更新已有记录
                        target.title = mt.isEmpty() ? "未命名的一天" : mt;
                        target.body = bt;
                        target.mood = mo.getText().toString().trim();
                        target.tag = ta.getText().toString().trim();
                        target.setImageUris(uris);
                    } else {
                        // 新建记录
                        MemoryRecord rec = new MemoryRecord();
                        rec.id = String.valueOf(System.currentTimeMillis());
                        rec.date = date.format(DATE_KEY);
                        rec.title = mt.isEmpty() ? "未命名的一天" : mt;
                        rec.body = bt;
                        rec.mood = mo.getText().toString().trim();
                        rec.tag = ta.getText().toString().trim();
                        rec.setImageUris(uris);
                        records.add(rec);
                    }
                    saveRecords();
                    refreshCurrentTab();
                }).show();
    }

    private void refreshImageStrip() {
        if (imageStripRow == null) return;
        imageStripRow.removeAllViews();
        for (int i = 0; i < pendingImageUris.size(); i++) {
            final int idx = i;
            Uri uri = pendingImageUris.get(i);
            FrameLayout wrap = new FrameLayout(this);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(dp(68), dp(68));
            wp.rightMargin = dp(8);
            wrap.setLayoutParams(wp);

            ImageView thumb = new ImageView(this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setImageURI(uri);
            thumb.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            wrap.addView(thumb);

            // 删除按钮
            TextView x = new TextView(this);
            x.setText("✕");
            x.setTextSize(10);
            x.setTextColor(Color.WHITE);
            x.setGravity(Gravity.CENTER);
            x.setBackgroundColor(0xCC000000);
            FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(dp(18), dp(18));
            xp.gravity = Gravity.TOP | Gravity.END;
            xp.topMargin = dp(4);
            xp.rightMargin = dp(4);
            x.setOnClickListener(v -> {
                pendingImageUris.remove(idx);
                refreshImageStrip();
            });
            wrap.addView(x, xp);

            imageStripRow.addView(wrap);
        }
    }

    private EditText styledInput(String hint, boolean multiLine) {
        EditText inp = new EditText(this);
        inp.setHint(hint); inp.setTextSize(15);
        inp.setTextColor(getColorByName("ink"));
        inp.setHintTextColor(getColorByName("muted_light"));
        inp.setBackground(getDrawableByName("input_bg"));
        inp.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (multiLine) {
            inp.setMinLines(4); inp.setGravity(Gravity.TOP);
            inp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
        inp.setLayoutParams(fullWidthMargin(0, dp(12)));
        return inp;
    }

    // ═══════════════════════════════════════════
    //  辅助功能
    // ═══════════════════════════════════════════

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int rc, int resultCode, Intent data) {
        super.onActivityResult(rc, resultCode, data);
        if (rc == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            int added = 0;
            // 多选：从 ClipData 获取
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null) { grantPersist(uri); pendingImageUris.add(uri); added++; }
                }
            } else if (data.getData() != null) {
                // 单选兼容
                Uri uri = data.getData();
                grantPersist(uri);
                pendingImageUris.add(uri);
                added++;
            }
            if (added > 0) {
                Toast.makeText(this, "已添加 " + added + " 张照片", Toast.LENGTH_SHORT).show();
                refreshImageStrip();
            }
        }
    }

    private void grantPersist(Uri uri) {
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (SecurityException ignored) {}
    }

    private void confirmDelete(MemoryRecord r) {
        new AlertDialog.Builder(this)
                .setTitle("删除这条记录？").setMessage("删除后本机将不再显示。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> { records.remove(r); saveRecords(); refreshCurrentTab(); })
                .show();
    }

    private void editBirthYear() {
        LinearLayout ct = new LinearLayout(this);
        ct.setPadding(dp(24), dp(12), dp(24), dp(8));
        EditText inp = styledInput("例如：1996", false);
        inp.setInputType(InputType.TYPE_CLASS_NUMBER);
        inp.setText(String.valueOf(prefs.getInt("birth_year", LocalDate.now().getYear() - 30)));
        ct.addView(inp);
        new AlertDialog.Builder(this).setTitle("出生年份").setView(ct)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    try { prefs.edit().putInt("birth_year", Integer.parseInt(inp.getText().toString())).apply(); refreshCurrentTab(); }
                    catch (NumberFormatException e) { Toast.makeText(this, "年份格式不正确", Toast.LENGTH_SHORT).show(); }
                }).show();
    }

    private void refreshCurrentTab() {
        if (currentTab == 0) showCalendar();
        else if (currentTab == 1) showTimeline();
        else if (currentTab == 2) showCentury();
        else showProfile();
    }

    // ═══════════════════════════════════════════
    //  数据层
    // ═══════════════════════════════════════════

    private void loadRecords() {
        records.clear();
        try {
            JSONArray arr = new JSONArray(prefs.getString("records", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MemoryRecord r = new MemoryRecord();
                r.id = o.optString("id"); r.date = o.optString("date");
                r.title = o.optString("title"); r.body = o.optString("body");
                r.mood = o.optString("mood"); r.tag = o.optString("tag");
                r.imageUris = o.optString("imageUris", o.optString("imageUri", ""));
                records.add(r);
            }
        } catch (JSONException ignored) {}
    }

    private void saveRecords() {
        JSONArray arr = new JSONArray();
        for (MemoryRecord r : records) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", r.id); o.put("date", r.date); o.put("title", r.title);
                o.put("body", r.body); o.put("mood", r.mood); o.put("tag", r.tag);
                o.put("imageUris", r.imageUris);
                arr.put(o);
            } catch (JSONException ignored) {}
        }
        prefs.edit().putString("records", arr.toString()).apply();
    }

    private List<MemoryRecord> recordsFor(LocalDate d) {
        String k = d.format(DATE_KEY);
        List<MemoryRecord> res = new ArrayList<>();
        for (MemoryRecord r : records) if (k.equals(r.date)) res.add(r);
        return res;
    }

    private int recordCountForYear(int y) {
        int c = 0; String p = y + "-";
        for (MemoryRecord r : records) if (r.date.startsWith(p)) c++;
        return c;
    }

    private int countRecordedDays() {
        Map<String, Boolean> m = new HashMap<>();
        for (MemoryRecord r : records) m.put(r.date, true);
        return m.size();
    }

    // ═══════════════════════════════════════════
    //  UI 微件工厂
    // ═══════════════════════════════════════════

    private TextView pageTitle(String v) {
        TextView tv = new TextView(this);
        tv.setText(v); tv.setTextColor(getColorByName("ink"));
        tv.setTextSize(28); tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private TextView pageDesc(String v) {
        TextView tv = new TextView(this);
        tv.setText(v); tv.setTextColor(getColorByName("muted"));
        tv.setTextSize(14); tv.setPadding(0, 0, 0, dp(18));
        return tv;
    }

    private View chip(String v) {
        TextView tv = new TextView(this);
        tv.setText(v); tv.setTextColor(getColorByName("accent"));
        tv.setTextSize(12); tv.setPadding(dp(12), dp(6), dp(12), dp(6));
        tv.setBackground(getDrawableByName("chip_bg"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(4), dp(8), 0);
        tv.setLayoutParams(p);
        return tv;
    }

    private View statRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackground(getDrawableByName("card_bg"));

        TextView lb = new TextView(this);
        lb.setText(label); lb.setTextColor(getColorByName("ink")); lb.setTextSize(15);
        row.addView(lb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView vl = new TextView(this);
        vl.setText(value); vl.setTextColor(getColorByName("accent"));
        vl.setTextSize(18); vl.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(vl);

        row.setLayoutParams(fullWidthMargin(0, dp(8)));
        return row;
    }

    private Button navButton(String label, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(label); btn.setTextSize(14); btn.setAllCaps(false);
        btn.setOnClickListener(listener); btn.setMinWidth(0); btn.setMinimumWidth(0);
        btn.setPadding(dp(12), dp(8), dp(12), dp(8));
        btn.setTextColor(getColorByName("ink_soft"));
        btn.setBackgroundColor(Color.TRANSPARENT);
        return btn;
    }

    private View emptyState(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg); tv.setTextColor(getColorByName("muted"));
        tv.setTextSize(14); tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(28), dp(40), dp(28), dp(40));
        tv.setBackground(getDrawableByName("card_elevated"));
        tv.setLineSpacing(dp(6), 1f);
        tv.setLayoutParams(fullWidthMargin(0, dp(18)));
        return tv;
    }

    private LinearLayout pageBody() {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(20), dp(20), dp(20), dp(24));
        return b;
    }

    private GridLayout.LayoutParams gridCellParam() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0; p.height = dp(56);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(3), dp(3), dp(3), dp(3));
        return p;
    }

    private LinearLayout.LayoutParams fullWidthMargin(int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, top, 0, bottom);
        return p;
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private android.graphics.drawable.Drawable getDrawableByName(String name) {
        return getResources().getDrawable(getResources().getIdentifier(
                name, "drawable", getPackageName()), getTheme());
    }

    private int getColorByName(String name) {
        return getResources().getColor(getResources().getIdentifier(
                name, "color", getPackageName()), getTheme());
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static class MemoryRecord {
        String id = "", date = "", title = "", body = "", mood = "", tag = "", imageUris = "";

        List<String> getImageUris() {
            List<String> list = new ArrayList<>();
            if (imageUris == null || imageUris.isEmpty()) return list;
            for (String s : imageUris.split("\\|")) {
                String t = s.trim();
                if (!t.isEmpty()) list.add(t);
            }
            return list;
        }

        void setImageUris(List<String> uris) {
            imageUris = uris == null || uris.isEmpty() ? "" : String.join("|", uris);
        }
    }
}
