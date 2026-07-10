package com.bainian.memory.util;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * 通用 UI 工具方法：dp 换算、按资源名取颜色/drawable、生成简单 drawable 等。
 * 所有方法都基于 Context，避免在各个 UI 类里重复反射资源查找逻辑。
 */
public final class UiKit {

    private UiKit() {}

    public static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    public static Drawable getDrawableByName(Context ctx, String name) {
        int resId = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
        return ctx.getResources().getDrawable(resId, ctx.getTheme());
    }

    public static int getColorByName(Context ctx, String name) {
        int resId = ctx.getResources().getIdentifier(name, "color", ctx.getPackageName());
        return ctx.getResources().getColor(resId, ctx.getTheme());
    }
}
