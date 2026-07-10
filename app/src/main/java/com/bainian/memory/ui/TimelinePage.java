package com.bainian.memory.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 时间线：像相册一样，按时间倒序回看所有记忆。
 * 左侧一条贯穿全程的时间轴线 + 年月锚点圆点，右侧是图片优先的记忆卡片，
 * 让「回看人生轨迹」这件事本身有仪式感，而不只是一份清单。
 */
public class TimelinePage {
    private final Context ctx;
    private final RecordRepository repo;
    private final PageHost host;

    public TimelinePage(Context ctx, RecordRepository repo, PageHost host) {
        this.ctx = ctx;
        this.repo = repo;
        this.host = host;
    }

    public View build() {
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout body = WidgetFactory.pageBody(ctx);
        scroll.addView(body);

        body.addView(WidgetFactory.pageTitle(ctx, "时间线"));
        body.addView(WidgetFactory.pageDesc(ctx, "像翻相册一样，回看走过的路。"));

        List<MemoryRecord> sorted = new ArrayList<>(repo.getAll());
        Collections.sort(sorted, (a, b) -> b.date.compareTo(a.date));
        if (sorted.isEmpty()) {
            body.addView(WidgetFactory.emptyState(ctx, "还没有任何记录。先去记录第一件值得记住的事。"));
            return scroll;
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

        String lastMonth = "";
        int total = sorted.size();
        for (int i = 0; i < total; i++) {
            MemoryRecord r = sorted.get(i);
            String month = r.date.substring(0, 7);
            boolean isMonthStart = !month.equals(lastMonth);
            if (isMonthStart) {
                lastMonth = month;
            }
            boolean isLast = i == total - 1;
            body.addView(timelineRow(r, isMonthStart, isLast, callbacks));
        }
        return scroll;
    }

    /** 单行：左侧轴线 + 圆点（月首放大并标注月份），右侧记忆卡片 */
    private View timelineRow(MemoryRecord r, boolean isMonthStart, boolean isLast, RecordCardFactory.Callbacks callbacks) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);

        int railWidth = UiKit.dp(ctx, 46);
        FrameLayout rail = new FrameLayout(ctx);

        // 竖线：贯穿整行（最后一条记录截断在圆点位置，避免拖出一截尾巴）
        View line = new View(ctx);
        line.setBackgroundColor(UiKit.getColorByName(ctx, "muted_light"));
        FrameLayout.LayoutParams lineLp = new FrameLayout.LayoutParams(UiKit.dp(ctx, 2), ViewGroup.LayoutParams.MATCH_PARENT);
        lineLp.gravity = Gravity.CENTER_HORIZONTAL;
        if (isLast) {
            lineLp.height = UiKit.dp(ctx, 28);
            lineLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        }
        rail.addView(line, lineLp);

        // 圆点：月首用强调色大圆点 + 月份文字，其余用小圆点
        if (isMonthStart) {
            LinearLayout dotWrap = new LinearLayout(ctx);
            dotWrap.setOrientation(LinearLayout.VERTICAL);
            dotWrap.setGravity(Gravity.CENTER_HORIZONTAL);
            dotWrap.setPadding(0, UiKit.dp(ctx, 4), 0, 0);

            View dot = new View(ctx);
            dot.setBackground(UiKit.getDrawableByName(ctx, "day_dot"));
            int dotSize = UiKit.dp(ctx, 12);
            dotWrap.addView(dot, new LinearLayout.LayoutParams(dotSize, dotSize));

            FrameLayout.LayoutParams dwLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dwLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            rail.addView(dotWrap, dwLp);
        } else {
            View smallDot = new View(ctx);
            smallDot.setBackgroundColor(UiKit.getColorByName(ctx, "muted_light"));
            int size = UiKit.dp(ctx, 6);
            FrameLayout.LayoutParams sdLp = new FrameLayout.LayoutParams(size, size);
            sdLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            sdLp.topMargin = UiKit.dp(ctx, 24);
            smallDot.setBackground(UiKit.getDrawableByName(ctx, "day_dot"));
            rail.addView(smallDot, sdLp);
        }

        row.addView(rail, new LinearLayout.LayoutParams(railWidth, ViewGroup.LayoutParams.MATCH_PARENT));

        // 右侧内容列：月首显示月份大标题 + 卡片
        LinearLayout contentCol = new LinearLayout(ctx);
        contentCol.setOrientation(LinearLayout.VERTICAL);
        if (isMonthStart) {
            TextView monthLabel = new TextView(ctx);
            String[] parts = r.date.split("-");
            monthLabel.setText(parts[0] + "年" + Integer.parseInt(parts[1]) + "月");
            monthLabel.setTextColor(UiKit.getColorByName(ctx, "ink"));
            monthLabel.setTextSize(18);
            monthLabel.setTypeface(Typeface.DEFAULT_BOLD);
            monthLabel.setPadding(0, UiKit.dp(ctx, 2), 0, UiKit.dp(ctx, 10));
            contentCol.addView(monthLabel);
        }
        contentCol.addView(RecordCardFactory.create(ctx, repo, r, false, callbacks));
        row.addView(contentCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return row;
    }
}
