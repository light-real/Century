package com.bainian.memory.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;

import java.time.LocalDate;

/** 记录/资料相关的共用弹窗动作：删除确认、出生年份编辑 */
public final class RecordActions {

    private RecordActions() {}

    public static void confirmDelete(Context ctx, RecordRepository repo, MemoryRecord r, PageHost host) {
        new AlertDialog.Builder(ctx)
                .setTitle("删除这条记录？")
                .setMessage("删除后本机将不再显示。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    repo.remove(r);
                    host.refreshCurrentTab();
                })
                .show();
    }

    /** 事后为一条记录补充/修改标签——记录当下不强制填写，回看时才需要整理分类 */
    public static void editTag(Context ctx, RecordRepository repo, MemoryRecord r, Runnable onSaved) {
        LinearLayout ct = new LinearLayout(ctx);
        ct.setPadding(UiKit.dp(ctx, 24), UiKit.dp(ctx, 12), UiKit.dp(ctx, 24), UiKit.dp(ctx, 8));
        EditText inp = WidgetFactory.styledInput(ctx, "例如：家人、旅行、工作", false);
        inp.setText(r.tag);
        ct.addView(inp);
        new AlertDialog.Builder(ctx).setTitle("添加标签").setView(ct)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    r.tag = inp.getText().toString().trim();
                    repo.update();
                    onSaved.run();
                }).show();
    }

    public static void editBirthYear(Context ctx, RecordRepository repo, PageHost host) {
        LinearLayout ct = new LinearLayout(ctx);
        ct.setPadding(UiKit.dp(ctx, 24), UiKit.dp(ctx, 12), UiKit.dp(ctx, 24), UiKit.dp(ctx, 8));
        EditText inp = WidgetFactory.styledInput(ctx, "例如：1996", false);
        inp.setInputType(InputType.TYPE_CLASS_NUMBER);
        inp.setText(String.valueOf(repo.getBirthYear()));
        ct.addView(inp);
        new AlertDialog.Builder(ctx).setTitle("出生年份").setView(ct)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    try {
                        repo.setBirthYear(Integer.parseInt(inp.getText().toString()));
                        host.refreshCurrentTab();
                    } catch (NumberFormatException e) {
                        Toast.makeText(ctx, "年份格式不正确", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }
}
