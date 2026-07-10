package com.bainian.memory.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;
import com.bainian.memory.view.ProfileAvatarView;
import com.bainian.memory.view.SettingsIconView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * 新首页——「回忆」：这是打开 App 第一眼看到的页面，定位是「情感化的回忆入口」而不是工具台。
 * 结构：
 *   顶部栏：问候语 + 日历图标（次级入口）+ 头像图标（进入「我的」）
 *   「那年今日」：如果历史上同月同日有记录，用醒目卡片呈现，唤起怀旧感
 *   「最近记忆」：图片优先的瀑布流，最新的记忆排在最前
 *   浮动「+」：随手记录今天
 */
public class MemoryFeedPage {
    private final Context ctx;
    private final RecordRepository repo;
    private final PageHost host;

    public MemoryFeedPage(Context ctx, RecordRepository repo, PageHost host) {
        this.ctx = ctx;
        this.repo = repo;
        this.host = host;
    }

    public View build() {
        FrameLayout page = new FrameLayout(ctx);
        ScrollView scroll = new ScrollView(ctx);
        scroll.setClipToPadding(false);
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(UiKit.dp(ctx, 20), UiKit.dp(ctx, 18), UiKit.dp(ctx, 20), UiKit.dp(ctx, 110));
        scroll.addView(body);
        page.addView(scroll, WidgetFactory.match());

        body.addView(buildTopBar());

        List<MemoryRecord> onThisDay = repo.onThisDay(LocalDate.now());
        if (!onThisDay.isEmpty()) {
            body.addView(buildOnThisDaySection(onThisDay));
        }

        body.addView(buildRecentSection());

        // 浮动新增按钮
        TextView fab = new TextView(ctx);
        fab.setText("+");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(28);
        fab.setGravity(Gravity.CENTER);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setBackground(UiKit.getDrawableByName(ctx, "fab_elevated"));
        fab.setOnClickListener(v -> host.showRecordDialog(LocalDate.now(), null));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(UiKit.dp(ctx, 56), UiKit.dp(ctx, 56));
        fp.gravity = Gravity.BOTTOM | Gravity.END;
        fp.setMargins(0, 0, UiKit.dp(ctx, 24), UiKit.dp(ctx, 24));
        page.addView(fab, fp);

        return page;
    }

    // ── 顶部：问候语 + 日历入口 + 头像入口 ──
    private View buildTopBar() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, UiKit.dp(ctx, 18));

        LinearLayout titles = new LinearLayout(ctx);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView greeting = new TextView(ctx);
        greeting.setText(greetingText());
        greeting.setTextColor(UiKit.getColorByName(ctx, "muted"));
        greeting.setTextSize(13);
        titles.addView(greeting);

        TextView title = new TextView(ctx);
        title.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日")) + " · "
                + LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA));
        title.setTextColor(UiKit.getColorByName(ctx, "ink"));
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(title);

        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // 日历图标入口——用手绘矢量图标替代 emoji，与「我的」设置列表、头像相机角标
        // 统一同一套几何图标语言，而不是各页面各画各的符号。
        FrameLayout calBtn = new FrameLayout(ctx);
        calBtn.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        int iconSize = UiKit.dp(ctx, 40);
        LinearLayout.LayoutParams calLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        calLp.setMarginEnd(UiKit.dp(ctx, 10));
        calBtn.setLayoutParams(calLp);
        SettingsIconView calIcon = new SettingsIconView(ctx);
        calIcon.setKind(SettingsIconView.Kind.CALENDAR);
        calIcon.setIconColor(UiKit.getColorByName(ctx, "ink_soft"));
        int calIconSize = UiKit.dp(ctx, 18);
        FrameLayout.LayoutParams calIconLp = new FrameLayout.LayoutParams(calIconSize, calIconSize);
        calIconLp.gravity = Gravity.CENTER;
        calBtn.addView(calIcon, calIconLp);
        calBtn.setOnClickListener(v -> host.openCalendar());
        row.addView(calBtn);

        // 头像入口 —— 进入「我的」，若已设置自定义头像则同步展示
        FrameLayout avatarWrap = new FrameLayout(ctx);
        ProfileAvatarView avatar = new ProfileAvatarView(ctx);
        avatar.setSizeAndColor(iconSize, UiKit.getColorByName(ctx, "accent"));
        avatar.setAvatarUri(ctx, repo.getAvatarUri());
        avatarWrap.addView(avatar, new FrameLayout.LayoutParams(iconSize, iconSize));
        avatarWrap.setOnClickListener(v -> host.openProfile());
        row.addView(avatarWrap, new LinearLayout.LayoutParams(iconSize, iconSize));

        return row;
    }

    private String greetingText() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 6) return "夜深了";
        if (hour < 11) return "早上好";
        if (hour < 14) return "中午好";
        if (hour < 18) return "下午好";
        return "晚上好";
    }

    // ── 那年今日 ──
    private View buildOnThisDaySection(List<MemoryRecord> records) {
        LinearLayout section = new LinearLayout(ctx);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 24)));

        LinearLayout header = new LinearLayout(ctx);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, UiKit.dp(ctx, 10));
        SettingsIconView icon = new SettingsIconView(ctx);
        icon.setKind(SettingsIconView.Kind.SPARK);
        icon.setIconColor(UiKit.getColorByName(ctx, "warm"));
        int sparkSize = UiKit.dp(ctx, 16);
        LinearLayout.LayoutParams sparkLp = new LinearLayout.LayoutParams(sparkSize, sparkSize);
        sparkLp.setMarginEnd(UiKit.dp(ctx, 6));
        icon.setLayoutParams(sparkLp);
        header.addView(icon);
        TextView label = new TextView(ctx);
        label.setText("那年今日");
        label.setTextColor(UiKit.getColorByName(ctx, "ink"));
        label.setTextSize(17);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(label);
        section.addView(header);

        if (records.size() == 1) {
            section.addView(onThisDayCard(records.get(0), true));
        } else {
            // 多条：横向滑动卡片
            HorizontalScrollView hs = new HorizontalScrollView(ctx);
            hs.setClipToPadding(false);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < records.size(); i++) {
                View card = onThisDayCard(records.get(i), false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        UiKit.dp(ctx, 240), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd(UiKit.dp(ctx, 12));
                row.addView(card, lp);
            }
            hs.addView(row);
            section.addView(hs);
        }
        return section;
    }

    private View onThisDayCard(MemoryRecord r, boolean fullWidth) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        card.setOnClickListener(v -> host.showRecordDialog(LocalDate.parse(r.date), r));

        List<String> uris = r.getImageUris();
        if (!uris.isEmpty()) {
            android.widget.ImageView img = new android.widget.ImageView(ctx);
            img.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            img.setImageURI(android.net.Uri.parse(uris.get(0)));
            FrameLayout imgWrap = new FrameLayout(ctx);
            imgWrap.setClipToOutline(true);
            imgWrap.addView(img, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, fullWidth ? 180 : 140)));
            card.addView(imgWrap);
        }

        LinearLayout textArea = new LinearLayout(ctx);
        textArea.setOrientation(LinearLayout.VERTICAL);
        textArea.setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 14), UiKit.dp(ctx, 16), UiKit.dp(ctx, 14));

        int yearsAgo = LocalDate.now().getYear() - LocalDate.parse(r.date).getYear();
        TextView yearTag = new TextView(ctx);
        yearTag.setText(yearsAgo + " 年前的今天");
        yearTag.setTextColor(UiKit.getColorByName(ctx, "accent"));
        yearTag.setTextSize(12);
        yearTag.setTypeface(Typeface.DEFAULT_BOLD);
        textArea.addView(yearTag);

        TextView body = new TextView(ctx);
        String bodyText = r.body == null || r.body.isEmpty() ? r.title : r.body;
        body.setText(bodyText);
        body.setTextColor(UiKit.getColorByName(ctx, "ink"));
        body.setTextSize(14);
        body.setMaxLines(fullWidth ? 3 : 2);
        body.setEllipsize(android.text.TextUtils.TruncateAt.END);
        body.setPadding(0, UiKit.dp(ctx, 6), 0, 0);
        textArea.addView(body);

        card.addView(textArea);

        LinearLayout.LayoutParams clp = fullWidth
                ? WidgetFactory.fullWidthMargin(ctx, 0, 0)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(clp);
        return card;
    }

    // ── 最近记忆瀑布流 ──
    private View buildRecentSection() {
        LinearLayout section = new LinearLayout(ctx);
        section.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(ctx);
        label.setText("最近的记忆");
        label.setTextColor(UiKit.getColorByName(ctx, "ink"));
        label.setTextSize(17);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, 0, 0, UiKit.dp(ctx, 10));
        section.addView(label);

        List<MemoryRecord> recent = repo.recent(30);
        if (recent.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("这里空空如也。\n点击右下角「+」，记录第一件值得记住的事。");
            empty.setTextColor(UiKit.getColorByName(ctx, "muted"));
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(UiKit.dp(ctx, 28), UiKit.dp(ctx, 40), UiKit.dp(ctx, 28), UiKit.dp(ctx, 40));
            empty.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
            empty.setLineSpacing(UiKit.dp(ctx, 6), 1f);
            section.addView(empty);
            return section;
        }

        RecordCardFactory.Callbacks callbacks = new RecordCardFactory.Callbacks() {
            @Override
            public void onEdit(LocalDate date, MemoryRecord record) {
                host.showRecordDialog(date, record);
            }

            @Override
            public void onDeleteRequested(MemoryRecord record) {
                RecordActions.confirmDelete(ctx, repo, record, host);
            }

            @Override
            public void onRefresh() {
                host.refreshCurrentTab();
            }
        };

        String lastMonthKey = "";
        for (MemoryRecord r : recent) {
            String monthKey = r.date.substring(0, 7);
            if (!monthKey.equals(lastMonthKey)) {
                section.addView(monthDivider(LocalDate.parse(r.date)));
                lastMonthKey = monthKey;
            }
            section.addView(RecordCardFactory.create(ctx, repo, r, true, callbacks));
        }
        return section;
    }

    private View monthDivider(LocalDate date) {
        TextView tv = new TextView(ctx);
        tv.setText(date.format(DateTimeFormatter.ofPattern("yyyy年M月")));
        tv.setTextColor(UiKit.getColorByName(ctx, "muted"));
        tv.setTextSize(12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, UiKit.dp(ctx, 14), 0, UiKit.dp(ctx, 8));
        return tv;
    }
}
