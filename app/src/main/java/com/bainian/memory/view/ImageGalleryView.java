package com.bainian.memory.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bainian.memory.util.UiKit;

import java.util.List;

/**
 * 记录卡片里的图片九宫格布局构建器。
 * 根据图片数量（1/2/3/4+）采用不同的排布方式，并支持点击全屏查看。
 */
public final class ImageGalleryView {

    private ImageGalleryView() {}

    public interface OnImageClickListener {
        void onClick(Uri uri);
    }

    public static View build(Context ctx, List<String> uris, OnImageClickListener listener) {
        int n = uris.size();
        int gap = UiKit.dp(ctx, 3);
        Drawable bg = UiKit.getDrawableByName(ctx, "card_bg");

        if (n == 1) {
            return buildSingle(ctx, uris, bg, listener);
        }
        if (n == 2) {
            return buildDouble(ctx, uris, bg, gap, listener);
        }
        if (n == 3) {
            return buildTriple(ctx, uris, bg, gap, listener);
        }
        return buildGrid(ctx, uris, bg, gap, listener);
    }

    private static View buildSingle(Context ctx, List<String> uris, Drawable bg, OnImageClickListener listener) {
        // 单图：全宽 240dp
        FrameLayout wrap = new FrameLayout(ctx);
        wrap.setClipToOutline(true);
        wrap.setBackground(bg);
        ImageView img = new ImageView(ctx);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Uri u0 = Uri.parse(uris.get(0));
        img.setImageURI(u0);
        img.setOnClickListener(v -> listener.onClick(u0));
        wrap.addView(img, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, 240)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, UiKit.dp(ctx, 2), 0, 0);
        wrap.setLayoutParams(lp);
        return wrap;
    }

    private static View buildDouble(Context ctx, List<String> uris, Drawable bg, int gap, OnImageClickListener listener) {
        // 双图：左右并排，各 50%
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 2; i++) {
            FrameLayout wrap = new FrameLayout(ctx);
            wrap.setClipToOutline(true);
            wrap.setBackground(bg);
            ImageView img = new ImageView(ctx);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Uri u = Uri.parse(uris.get(i));
            img.setImageURI(u);
            img.setOnClickListener(v -> listener.onClick(u));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiKit.dp(ctx, 200), 1f);
            if (i == 0) lp.rightMargin = gap;
            wrap.addView(img, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            row.addView(wrap, lp);
        }
        return row;
    }

    private static View buildTriple(Context ctx, List<String> uris, Drawable bg, int gap, OnImageClickListener listener) {
        // 三图：左大(66%) + 右二叠(stack)
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        // 左图
        FrameLayout left = new FrameLayout(ctx);
        left.setClipToOutline(true);
        left.setBackground(bg);
        ImageView li = new ImageView(ctx);
        li.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Uri lu = Uri.parse(uris.get(0));
        li.setImageURI(lu);
        li.setOnClickListener(v -> listener.onClick(lu));
        left.addView(li, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        row.addView(left, new LinearLayout.LayoutParams(0, UiKit.dp(ctx, 200), 0.66f));
        // 右侧间距
        View sp = new View(ctx);
        row.addView(sp, new LinearLayout.LayoutParams(gap, 0));
        // 右侧两张叠图
        LinearLayout right = new LinearLayout(ctx);
        right.setOrientation(LinearLayout.VERTICAL);
        for (int i = 1; i < 3; i++) {
            FrameLayout wrap = new FrameLayout(ctx);
            wrap.setClipToOutline(true);
            wrap.setBackground(bg);
            ImageView img = new ImageView(ctx);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Uri ru = Uri.parse(uris.get(i));
            img.setImageURI(ru);
            img.setOnClickListener(v -> listener.onClick(ru));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            if (i == 2) rp.topMargin = gap;
            wrap.addView(img, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            right.addView(wrap, rp);
        }
        row.addView(right, new LinearLayout.LayoutParams(0, UiKit.dp(ctx, 200), 0.34f));
        return row;
    }

    private static View buildGrid(Context ctx, List<String> uris, Drawable bg, int gap, OnImageClickListener listener) {
        // 4+ 图：2 列网格，显示前 4 或前 3 + "+N"
        int n = uris.size();
        int show = Math.min(n, 4);
        int cols = 2;
        int rows = (int) Math.ceil(show / 2.0);
        int cellH = UiKit.dp(ctx, 120);
        LinearLayout grid = new LinearLayout(ctx);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            if (r > 0) row.setPadding(0, gap, 0, 0);
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                FrameLayout wrap = new FrameLayout(ctx);
                wrap.setClipToOutline(true);
                wrap.setBackground(bg);
                ImageView img = new ImageView(ctx);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (idx < uris.size()) {
                    Uri gu = Uri.parse(uris.get(idx));
                    img.setImageURI(gu);
                    img.setOnClickListener(v -> listener.onClick(gu));
                }
                wrap.addView(img, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                // "+N" overlay on last visible cell when there are more
                if (idx == show - 1 && n > show) {
                    TextView overlay = new TextView(ctx);
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
}
