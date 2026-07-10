package com.bainian.memory.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.view.View;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 日历页：月历网格 + 当日记录列表 + 新增按钮。
 * 现在作为次级页面使用（从首页「回忆」右上角日历图标进入），支持传入 onBack 显示返回箭头；
 * 页面内部的翻月/切日不再整页刷新，而是通过 rebuild 回调局部重建，体验更连贯。
 */
public class CalendarPage {
    private final Context ctx;
    private final RecordRepository repo;
    private final PageHost host;
    private final Runnable onBack;
    private Runnable rebuild = () -> {};

    public CalendarPage(Context ctx, RecordRepository repo, PageHost host) {
        this(ctx, repo, host, null);
    }

    public CalendarPage(Context ctx, RecordRepository repo, PageHost host, Runnable onBack) {
        this.ctx = ctx;
        this.repo = repo;
        this.host = host;
        this.onBack = onBack;
    }

    /** 供宿主容器传入「原地重建整页」的回调，翻月/切换选中日期时调用，避免影响外层导航状态 */
    public void setRebuildCallback(Runnable rebuild) {
        this.rebuild = rebuild;
    }

    public View build() {
        FrameLayout page = new FrameLayout(ctx);
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(UiKit.dp(ctx, 20), UiKit.dp(ctx, 20), UiKit.dp(ctx, 20), UiKit.dp(ctx, 100));
        scroll.addView(body);
        page.addView(scroll, WidgetFactory.match());

        YearMonth visibleMonth = host.getVisibleMonth();
        LocalDate selectedDate = host.getSelectedDate();

        // 顶部：返回 + 月份标题
        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, 0, 0, UiKit.dp(ctx, 4));
        if (onBack != null) {
            TextView back = new TextView(ctx);
            back.setText("‹");
            back.setTextSize(28);
            back.setTextColor(UiKit.getColorByName(ctx, "ink"));
            back.setPadding(0, 0, UiKit.dp(ctx, 12), 0);
            back.setOnClickListener(v -> onBack.run());
            topRow.addView(back);
        }
        TextView pageLabel = new TextView(ctx);
        pageLabel.setText("日历");
        pageLabel.setTextSize(15);
        pageLabel.setTextColor(UiKit.getColorByName(ctx, "muted"));
        topRow.addView(pageLabel);
        body.addView(topRow, WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 8)));

        // 月份标题
        LinearLayout header = new LinearLayout(ctx);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, UiKit.dp(ctx, 14));
        TextView title = new TextView(ctx);
        title.setText(visibleMonth.getYear() + "年" + visibleMonth.getMonthValue() + "月");
        title.setTextColor(UiKit.getColorByName(ctx, "ink"));
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(WidgetFactory.navButton(ctx, "◀", v -> {
            host.setVisibleMonth(host.getVisibleMonth().minusMonths(1));
            rebuild.run();
        }));
        header.addView(WidgetFactory.navButton(ctx, "今天", v -> {
            host.setSelectedDate(LocalDate.now());
            host.setVisibleMonth(YearMonth.now());
            rebuild.run();
        }));
        header.addView(WidgetFactory.navButton(ctx, "▶", v -> {
            host.setVisibleMonth(host.getVisibleMonth().plusMonths(1));
            rebuild.run();
        }));
        body.addView(header);

        // 日历网格
        LinearLayout calWrap = new LinearLayout(ctx);
        calWrap.setOrientation(LinearLayout.VERTICAL);
        calWrap.setPadding(UiKit.dp(ctx, 4), UiKit.dp(ctx, 14), UiKit.dp(ctx, 4), UiKit.dp(ctx, 14));
        calWrap.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));

        GridLayout weekBar = new GridLayout(ctx);
        weekBar.setColumnCount(7);
        for (String w : new String[]{"一", "二", "三", "四", "五", "六", "日"}) {
            TextView wl = new TextView(ctx);
            wl.setText(w);
            wl.setTextSize(12);
            wl.setTextColor(UiKit.getColorByName(ctx, "muted"));
            wl.setGravity(Gravity.CENTER);
            wl.setPadding(0, 0, 0, UiKit.dp(ctx, 6));
            weekBar.addView(wl, WidgetFactory.gridCellParam(ctx));
        }
        calWrap.addView(weekBar);

        GridLayout dayGrid = new GridLayout(ctx);
        dayGrid.setColumnCount(7);
        int leading = visibleMonth.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leading; i++) dayGrid.addView(new Space(ctx), WidgetFactory.gridCellParam(ctx));
        for (int d = 1; d <= visibleMonth.lengthOfMonth(); d++) {
            dayGrid.addView(dayCell(visibleMonth.atDay(d), selectedDate), WidgetFactory.gridCellParam(ctx));
        }
        calWrap.addView(dayGrid);
        body.addView(calWrap, WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 10)));

        // 日期标题 + 记录
        TextView dayH = new TextView(ctx);
        dayH.setText(selectedDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)));
        dayH.setTextColor(UiKit.getColorByName(ctx, "ink"));
        dayH.setTextSize(17);
        dayH.setTypeface(Typeface.DEFAULT_BOLD);
        dayH.setPadding(0, UiKit.dp(ctx, 14), 0, UiKit.dp(ctx, 10));
        body.addView(dayH);
        renderDayRecords(body, selectedDate);

        // FAB
        TextView fab = new TextView(ctx);
        fab.setText("+");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(28);
        fab.setGravity(Gravity.CENTER);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setBackground(UiKit.getDrawableByName(ctx, "fab_elevated"));
        fab.setOnClickListener(v -> host.showRecordDialog(host.getSelectedDate(), null));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(UiKit.dp(ctx, 56), UiKit.dp(ctx, 56));
        fp.gravity = Gravity.BOTTOM | Gravity.END;
        fp.setMargins(0, 0, UiKit.dp(ctx, 24), UiKit.dp(ctx, 24));
        page.addView(fab, fp);

        return page;
    }

    private View dayCell(LocalDate date, LocalDate selectedDate) {
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(UiKit.dp(ctx, 2), UiKit.dp(ctx, 4), UiKit.dp(ctx, 2), UiKit.dp(ctx, 4));
        boolean today = date.equals(LocalDate.now());
        boolean selected = date.equals(selectedDate);

        TextView num = new TextView(ctx);
        num.setText(String.valueOf(date.getDayOfMonth()));
        num.setTextSize(14);
        num.setGravity(Gravity.CENTER);

        if (today) {
            num.setTextColor(Color.WHITE);
            num.setBackground(UiKit.getDrawableByName(ctx, "calendar_today_bg"));
            num.setWidth(UiKit.dp(ctx, 34));
            num.setHeight(UiKit.dp(ctx, 34));
            num.setGravity(Gravity.CENTER);
            num.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (selected) {
            num.setTextColor(UiKit.getColorByName(ctx, "accent"));
            num.setBackground(UiKit.getDrawableByName(ctx, "calendar_selected_bg"));
            num.setWidth(UiKit.dp(ctx, 34));
            num.setHeight(UiKit.dp(ctx, 34));
            num.setGravity(Gravity.CENTER);
            num.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            num.setTextColor(UiKit.getColorByName(ctx, "ink"));
        }
        cell.addView(num);

        int count = repo.recordsFor(date).size();
        if (count > 0) {
            View dot = new View(ctx);
            dot.setBackground(UiKit.getDrawableByName(ctx, "day_dot"));
            cell.addView(dot, new LinearLayout.LayoutParams(UiKit.dp(ctx, 5), UiKit.dp(ctx, 5)));
        } else {
            cell.addView(new Space(ctx), new LinearLayout.LayoutParams(UiKit.dp(ctx, 5), UiKit.dp(ctx, 5)));
        }

        cell.setOnClickListener(v -> {
            host.setSelectedDate(date);
            rebuild.run();
        });
        return cell;
    }

    private void renderDayRecords(LinearLayout parent, LocalDate date) {
        List<MemoryRecord> dayRecords = repo.recordsFor(date);
        if (dayRecords.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("今天还没有记录。按右下角「+」，写下一件值得留下的事。");
            empty.setTextColor(UiKit.getColorByName(ctx, "muted"));
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(UiKit.dp(ctx, 28), UiKit.dp(ctx, 32), UiKit.dp(ctx, 28), UiKit.dp(ctx, 32));
            empty.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
            empty.setLineSpacing(UiKit.dp(ctx, 6), 1f);
            parent.addView(empty);
            return;
        }
        RecordCardFactory.Callbacks callbacks = new RecordCardFactory.Callbacks() {
            @Override
            public void onEdit(LocalDate d, MemoryRecord record) {
                host.showRecordDialog(d, record);
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
        for (MemoryRecord r : dayRecords) {
            parent.addView(RecordCardFactory.create(ctx, repo, r, true, callbacks));
        }
    }
}
