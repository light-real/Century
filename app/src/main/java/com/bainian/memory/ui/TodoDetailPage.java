package com.bainian.memory.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bainian.memory.data.TodoRepository;
import com.bainian.memory.model.TodoItem;
import com.bainian.memory.model.TodoPlan;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;
import com.bainian.memory.view.SettingsIconView;

import java.util.List;

/**
 * 「计划详情」页：某一个 TODO 计划下的任务清单，支持添加、勾选完成、删除、修改计划名称。
 *
 * 作为次级页面从 {@link TodoListPage} 进入，带返回箭头。
 */
public class TodoDetailPage {
    private final Context ctx;
    private final TodoRepository repo;
    private final PageHost host;
    private final String planId;
    private final Runnable onBack;
    private Runnable rebuild = () -> {};

    public TodoDetailPage(Context ctx, TodoRepository repo, PageHost host, String planId, Runnable onBack) {
        this.ctx = ctx;
        this.repo = repo;
        this.host = host;
        this.planId = planId;
        this.onBack = onBack;
    }

    public void setRebuildCallback(Runnable rebuild) {
        this.rebuild = rebuild;
    }

    public View build() {
        TodoPlan plan = repo.getPlan(planId);

        ScrollView scroll = new ScrollView(ctx);
        scroll.setClipToPadding(false);
        LinearLayout body = WidgetFactory.pageBody(ctx);
        body.setPadding(UiKit.dp(ctx, 20), UiKit.dp(ctx, 16), UiKit.dp(ctx, 20), UiKit.dp(ctx, 32));
        scroll.addView(body);

        // 返回箭头
        TextView back = new TextView(ctx);
        back.setText("‹ 返回");
        back.setTextSize(16);
        back.setTextColor(UiKit.getColorByName(ctx, "ink"));
        back.setPadding(0, 0, 0, UiKit.dp(ctx, 14));
        back.setOnClickListener(v -> {
            if (onBack != null) onBack.run();
        });
        body.addView(back);

        if (plan == null) {
            body.addView(WidgetFactory.emptyState(ctx, "这个计划已经不存在了"));
            return scroll;
        }

        // 标题行：计划名（点击可改名）+ 删除
        LinearLayout titleRow = new LinearLayout(ctx);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 4)));

        TextView title = WidgetFactory.pageTitle(ctx, TextUtils.isEmpty(plan.title) ? "未命名计划" : plan.title);
        title.setPadding(0, 0, 0, 0);
        title.setOnClickListener(v -> showRenameDialog(plan));
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView deleteBtn = new TextView(ctx);
        deleteBtn.setText("删除计划");
        deleteBtn.setTextSize(13);
        deleteBtn.setTextColor(UiKit.getColorByName(ctx, "muted"));
        deleteBtn.setPadding(UiKit.dp(ctx, 8), UiKit.dp(ctx, 8), 0, UiKit.dp(ctx, 8));
        deleteBtn.setOnClickListener(v -> showDeletePlanDialog(plan));
        titleRow.addView(deleteBtn);
        body.addView(titleRow);

        List<TodoItem> items = repo.getItems(planId);
        int doneCount = 0;
        for (TodoItem it : items) if (it.done) doneCount++;
        TextView desc = WidgetFactory.pageDesc(ctx,
                items.isEmpty() ? "还没有任务，添加第一条吧" : "已完成 " + doneCount + " / " + items.size());
        body.addView(desc);

        // 添加任务输入行
        body.addView(buildAddRow());

        if (items.isEmpty()) {
            body.addView(WidgetFactory.emptyState(ctx, "轻触上方输入框，添加一条任务"));
        } else {
            for (TodoItem it : items) {
                body.addView(itemRow(it));
            }
        }

        return scroll;
    }

    private View buildAddRow() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, UiKit.dp(ctx, 6), UiKit.dp(ctx, 16)));

        EditText input = WidgetFactory.styledInput(ctx, "添加一条任务，回车确认", false);
        input.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            submitNewItem(input);
            return true;
        });
        row.addView(input);

        TextView addBtn = new TextView(ctx);
        addBtn.setText("添加");
        addBtn.setTextSize(15);
        addBtn.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.setTextColor(UiKit.getColorByName(ctx, "accent"));
        addBtn.setPadding(UiKit.dp(ctx, 14), UiKit.dp(ctx, 12), UiKit.dp(ctx, 4), UiKit.dp(ctx, 12));
        addBtn.setOnClickListener(v -> submitNewItem(input));
        row.addView(addBtn);

        return row;
    }

    private void submitNewItem(EditText input) {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        repo.addItem(planId, text);
        input.setText("");
        rebuild.run();
    }

    private View itemRow(TodoItem item) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(ctx, 14), UiKit.dp(ctx, 14), UiKit.dp(ctx, 10), UiKit.dp(ctx, 14));
        row.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        row.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 10)));
        row.setOnClickListener(v -> {
            repo.toggleItemDone(item);
            rebuild.run();
        });
        row.setOnLongClickListener(v -> {
            showDeleteItemDialog(item);
            return true;
        });

        // 勾选圆圈
        FrameLayout checkWrap = new FrameLayout(ctx);
        View circle = new View(ctx);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        if (item.done) {
            gd.setColor(UiKit.getColorByName(ctx, "accent"));
        } else {
            gd.setColor(0x00000000);
            gd.setStroke(UiKit.dp(ctx, 2), UiKit.getColorByName(ctx, "muted_light"));
        }
        circle.setBackground(gd);
        int circleSize = UiKit.dp(ctx, 22);
        checkWrap.addView(circle, new FrameLayout.LayoutParams(circleSize, circleSize));
        if (item.done) {
            // 矢量对勾替代文本符号 ✓，与全局图标语言保持一致
            SettingsIconView check = new SettingsIconView(ctx);
            check.setKind(SettingsIconView.Kind.CHECK);
            check.setIconColor(0xFFFFFFFF);
            int checkIconSize = UiKit.dp(ctx, 13);
            FrameLayout.LayoutParams checkLp = new FrameLayout.LayoutParams(checkIconSize, checkIconSize);
            checkLp.gravity = Gravity.CENTER;
            checkWrap.addView(check, checkLp);
        }
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(circleSize, circleSize);
        checkLp.setMargins(0, 0, UiKit.dp(ctx, 14), 0);
        row.addView(checkWrap, checkLp);

        // 文本
        LinearLayout textArea = new LinearLayout(ctx);
        textArea.setOrientation(LinearLayout.VERTICAL);
        TextView textTv = new TextView(ctx);
        textTv.setText(item.text);
        textTv.setTextSize(15);
        textTv.setTextColor(item.done
                ? UiKit.getColorByName(ctx, "muted_light")
                : UiKit.getColorByName(ctx, "ink"));
        if (item.done) {
            textTv.setPaintFlags(textTv.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        textArea.addView(textTv);

        if (item.done && !TextUtils.isEmpty(item.doneByName)) {
            TextView doneHint = new TextView(ctx);
            doneHint.setText(item.doneByName + " 完成");
            doneHint.setTextSize(11);
            doneHint.setTextColor(UiKit.getColorByName(ctx, "muted"));
            doneHint.setPadding(0, UiKit.dp(ctx, 2), 0, 0);
            textArea.addView(doneHint);
        }

        row.addView(textArea, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return row;
    }

    private void showRenameDialog(TodoPlan plan) {
        LinearLayout ct = new LinearLayout(ctx);
        ct.setPadding(UiKit.dp(ctx, 24), UiKit.dp(ctx, 12), UiKit.dp(ctx, 24), UiKit.dp(ctx, 8));
        EditText inp = WidgetFactory.styledInput(ctx, "计划名称", false);
        inp.setText(plan.title);
        ct.addView(inp);
        new AlertDialog.Builder(ctx).setTitle("重命名计划").setView(ct)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    repo.renamePlan(planId, inp.getText().toString());
                    rebuild.run();
                }).show();
    }

    private void showDeletePlanDialog(TodoPlan plan) {
        new AlertDialog.Builder(ctx)
                .setTitle("删除「" + (TextUtils.isEmpty(plan.title) ? "未命名计划" : plan.title) + "」？")
                .setMessage("计划下的所有任务也会一并删除，且无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    repo.deletePlan(planId);
                    if (onBack != null) onBack.run();
                })
                .show();
    }

    private void showDeleteItemDialog(TodoItem item) {
        new AlertDialog.Builder(ctx)
                .setTitle("删除这条任务？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    repo.deleteItem(item.id);
                    rebuild.run();
                })
                .show();
    }
}
