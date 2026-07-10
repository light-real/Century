package com.bainian.memory.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.bainian.memory.util.UiKit.dp;
import static com.bainian.memory.util.UiKit.getColorByName;
import static com.bainian.memory.util.UiKit.getDrawableByName;

/**
 * 各页面共用的小控件工厂：标题、说明文字、空状态、chip、输入框、布局参数等。
 * 所有方法都是无状态的静态方法，接收 Context 即可复用。
 */
public final class WidgetFactory {

    private WidgetFactory() {}

    public static TextView pageTitle(Context ctx, String v) {
        TextView tv = new TextView(ctx);
        tv.setText(v);
        tv.setTextColor(getColorByName(ctx, "ink"));
        tv.setTextSize(28);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 0, 0, dp(ctx, 6));
        return tv;
    }

    public static TextView pageDesc(Context ctx, String v) {
        TextView tv = new TextView(ctx);
        tv.setText(v);
        tv.setTextColor(getColorByName(ctx, "muted"));
        tv.setTextSize(14);
        tv.setPadding(0, 0, 0, dp(ctx, 18));
        return tv;
    }

    public static View chip(Context ctx, String v) {
        TextView tv = new TextView(ctx);
        tv.setText(v);
        tv.setTextColor(getColorByName(ctx, "accent"));
        tv.setTextSize(12);
        tv.setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6));
        tv.setBackground(getDrawableByName(ctx, "chip_bg"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(ctx, 4), dp(ctx, 8), 0);
        tv.setLayoutParams(p);
        return tv;
    }

    public static View statRow(Context ctx, String label, String value) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 14), dp(ctx, 14), dp(ctx, 14), dp(ctx, 14));
        row.setBackground(getDrawableByName(ctx, "card_bg"));

        TextView lb = new TextView(ctx);
        lb.setText(label);
        lb.setTextColor(getColorByName(ctx, "ink"));
        lb.setTextSize(15);
        row.addView(lb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView vl = new TextView(ctx);
        vl.setText(value);
        vl.setTextColor(getColorByName(ctx, "accent"));
        vl.setTextSize(18);
        vl.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(vl);

        row.setLayoutParams(fullWidthMargin(ctx, 0, dp(ctx, 8)));
        return row;
    }

    public static Button navButton(Context ctx, String label, View.OnClickListener listener) {
        Button btn = new Button(ctx);
        btn.setText(label);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setOnClickListener(listener);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        btn.setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8));
        btn.setTextColor(getColorByName(ctx, "ink_soft"));
        btn.setBackgroundColor(Color.TRANSPARENT);
        return btn;
    }

    /**
     * 通用空状态卡片——之前只是一段居中文字堆在卡片里，略显单调、像未完成的占位符；
     * 现在加一枚极简的柔和圆点作为视觉锚点，让「这里还没有内容」看起来像是留白，
     * 而不是被遗忘的空白页，呼应「情感优先于效率」里对空状态也要有温度的要求。
     */
    public static View emptyState(Context ctx, String msg) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(ctx, 28), dp(ctx, 36), dp(ctx, 28), dp(ctx, 36));
        card.setBackground(getDrawableByName(ctx, "card_elevated"));
        card.setLayoutParams(fullWidthMargin(ctx, 0, dp(ctx, 18)));

        View dot = new View(ctx);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(getColorByName(ctx, "accent_soft"));
        dot.setBackground(gd);
        int dotSize = dp(ctx, 34);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotLp.bottomMargin = dp(ctx, 14);
        card.addView(dot, dotLp);

        TextView tv = new TextView(ctx);
        tv.setText(msg);
        tv.setTextColor(getColorByName(ctx, "muted"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setLineSpacing(dp(ctx, 6), 1f);
        card.addView(tv);

        return card;
    }

    public static LinearLayout pageBody(Context ctx) {
        LinearLayout b = new LinearLayout(ctx);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(ctx, 20), dp(ctx, 20), dp(ctx, 20), dp(ctx, 24));
        return b;
    }

    public static EditText styledInput(Context ctx, String hint, boolean multiLine) {
        EditText inp = new EditText(ctx);
        inp.setHint(hint);
        inp.setTextSize(15);
        inp.setTextColor(getColorByName(ctx, "ink"));
        inp.setHintTextColor(getColorByName(ctx, "muted_light"));
        inp.setBackground(getDrawableByName(ctx, "input_bg"));
        inp.setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14));
        if (multiLine) {
            inp.setMinLines(4);
            inp.setGravity(Gravity.TOP);
            inp.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
        inp.setLayoutParams(fullWidthMargin(ctx, 0, dp(ctx, 12)));
        return inp;
    }

    public static GridLayout.LayoutParams gridCellParam(Context ctx) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = dp(ctx, 56);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(ctx, 3), dp(ctx, 3), dp(ctx, 3), dp(ctx, 3));
        return p;
    }

    public static LinearLayout.LayoutParams fullWidthMargin(Context ctx, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, top, 0, bottom);
        return p;
    }

    public static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
