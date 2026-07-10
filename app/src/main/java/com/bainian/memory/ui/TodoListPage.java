package com.bainian.memory.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bainian.memory.data.TodoRepository;
import com.bainian.memory.model.TodoPlan;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;

import java.util.List;

/**
 * 「计划」列表页：展示本机全部 TODO 计划，支持新建、删除、进入某个计划查看任务。
 *
 * 作为底部导航常驻 tab 的主页面展示（与「回忆」「时间线」平级），因此不带返回箭头；
 * 点击某个计划卡片会以次级页面（overlay）形式打开任务详情，那一层才需要返回箭头。
 */
public class TodoListPage {
    private final Context ctx;
    private final TodoRepository repo;
    private final PageHost host;
    private final Runnable onBack; // 仅在作为次级页面被复用时才非空；作为 tab 主页展示时为 null
    private final java.util.function.Consumer<String> onOpenPlanCallback; // 点击某个计划卡片时回调，传出 planId
    private Runnable rebuild = () -> {};

    public TodoListPage(Context ctx, TodoRepository repo, PageHost host, Runnable onBack,
                         java.util.function.Consumer<String> onOpenPlan) {
        this.ctx = ctx;
        this.repo = repo;
        this.host = host;
        this.onBack = onBack;
        this.onOpenPlanCallback = onOpenPlan;
    }

    public void setRebuildCallback(Runnable rebuild) {
        this.rebuild = rebuild;
    }

    public View build() {
        ScrollView scroll = new ScrollView(ctx);
        scroll.setClipToPadding(false);
        LinearLayout body = WidgetFactory.pageBody(ctx);
        body.setPadding(UiKit.dp(ctx, 20), UiKit.dp(ctx, 16), UiKit.dp(ctx, 20), UiKit.dp(ctx, 32));
        scroll.addView(body);

        // 返回箭头：仅当作为次级页面被复用（onBack 非空）时才展示；
        // 作为底部 tab 主页展示时 onBack 为 null，不需要返回箭头。
        if (onBack != null) {
            TextView back = new TextView(ctx);
            back.setText("‹ 返回");
            back.setTextSize(16);
            back.setTextColor(UiKit.getColorByName(ctx, "ink"));
            back.setPadding(0, 0, 0, UiKit.dp(ctx, 14));
            back.setOnClickListener(v -> onBack.run());
            body.addView(back);
        }

        // 标题行 + 新建按钮
        LinearLayout titleRow = new LinearLayout(ctx);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 4)));

        TextView title = WidgetFactory.pageTitle(ctx, "计划");
        title.setPadding(0, 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView addBtn = new TextView(ctx);
        addBtn.setText("＋ 新建");
        addBtn.setTextSize(15);
        addBtn.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.setTextColor(UiKit.getColorByName(ctx, "accent"));
        addBtn.setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 8), UiKit.dp(ctx, 4), UiKit.dp(ctx, 8));
        addBtn.setOnClickListener(v -> showCreatePlanDialog());
        titleRow.addView(addBtn);
        body.addView(titleRow);

        TextView desc = WidgetFactory.pageDesc(ctx, "把想做的事列出来，一条条划掉。");
        body.addView(desc);

        List<TodoPlan> plans = repo.getAllPlans();
        if (plans.isEmpty()) {
            body.addView(WidgetFactory.emptyState(ctx, "还没有计划\n点击右上角「＋ 新建」创建第一个吧"));
        } else {
            for (TodoPlan p : plans) {
                body.addView(planCard(p));
            }
        }

        return scroll;
    }

    private View planCard(TodoPlan p) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(UiKit.dp(ctx, 18), UiKit.dp(ctx, 16), UiKit.dp(ctx, 18), UiKit.dp(ctx, 16));
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        card.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 12)));
        card.setOnClickListener(v -> {
            if (onOpenPlanCallback != null) onOpenPlanCallback.accept(p.id);
        });
        card.setOnLongClickListener(v -> {
            showDeletePlanDialog(p);
            return true;
        });

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(ctx);
        titleTv.setText(TextUtils.isEmpty(p.title) ? "未命名计划" : p.title);
        titleTv.setTextColor(UiKit.getColorByName(ctx, "ink"));
        titleTv.setTextSize(17);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView countTv = new TextView(ctx);
        countTv.setText(p.doneCount + "/" + p.totalCount);
        countTv.setTextColor(p.totalCount > 0 && p.doneCount == p.totalCount
                ? UiKit.getColorByName(ctx, "accent")
                : UiKit.getColorByName(ctx, "muted"));
        countTv.setTextSize(14);
        countTv.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(countTv);

        TextView arrow = new TextView(ctx);
        arrow.setText("›");
        arrow.setTextColor(UiKit.getColorByName(ctx, "muted"));
        arrow.setTextSize(20);
        arrow.setPadding(UiKit.dp(ctx, 8), 0, 0, 0);
        row.addView(arrow);

        card.addView(row);

        // 进度条
        if (p.totalCount > 0) {
            LinearLayout track = new LinearLayout(ctx);
            track.setOrientation(LinearLayout.HORIZONTAL);
            track.setBackground(UiKit.getDrawableByName(ctx, "progress_track"));
            track.setClipToOutline(true);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, 4));
            tp.setMargins(0, UiKit.dp(ctx, 12), 0, 0);
            track.setLayoutParams(tp);
            int doneWeight = p.doneCount;
            int remainWeight = p.totalCount - p.doneCount;
            View fill = new View(ctx);
            fill.setBackgroundColor(UiKit.getColorByName(ctx, "accent"));
            track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.max(doneWeight, 0)));
            View remain = new View(ctx);
            track.addView(remain, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.max(remainWeight, 0)));
            card.addView(track);
        } else {
            TextView emptyHint = new TextView(ctx);
            emptyHint.setText("还没有任务，点进去添加第一条吧");
            emptyHint.setTextColor(UiKit.getColorByName(ctx, "muted"));
            emptyHint.setTextSize(12);
            emptyHint.setPadding(0, UiKit.dp(ctx, 8), 0, 0);
            card.addView(emptyHint);
        }

        return card;
    }

    private void showCreatePlanDialog() {
        LinearLayout ct = new LinearLayout(ctx);
        ct.setPadding(UiKit.dp(ctx, 24), UiKit.dp(ctx, 12), UiKit.dp(ctx, 24), UiKit.dp(ctx, 8));
        EditText inp = WidgetFactory.styledInput(ctx, "例如：周末采购清单", false);
        ct.addView(inp);
        new AlertDialog.Builder(ctx).setTitle("新建计划").setView(ct)
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", (d, w) -> {
                    String title = inp.getText().toString().trim();
                    if (title.isEmpty()) title = "未命名计划";
                    TodoPlan p = repo.createPlan(title);
                    rebuild.run();
                    if (onOpenPlanCallback != null) onOpenPlanCallback.accept(p.id);
                }).show();
    }

    private void showDeletePlanDialog(TodoPlan p) {
        new AlertDialog.Builder(ctx)
                .setTitle("删除「" + (TextUtils.isEmpty(p.title) ? "未命名计划" : p.title) + "」？")
                .setMessage("计划下的所有任务也会一并删除，且无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    repo.deletePlan(p.id);
                    rebuild.run();
                })
                .show();
    }
}
