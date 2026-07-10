package com.bainian.memory.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bainian.memory.util.UiKit;
import com.bainian.memory.view.ZoomImageView;

/** 全屏图片查看弹窗，支持双指缩放与点击关闭 */
public final class ImageViewerDialog {

    private ImageViewerDialog() {}

    public static void show(Context ctx, Uri imageUri) {
        Dialog dialog = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(0xFF000000);
        root.setOnClickListener(v -> dialog.dismiss());

        ZoomImageView zoomView = new ZoomImageView(ctx);
        zoomView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        zoomView.setImageURI(imageUri);
        zoomView.setOnTapListener(dialog::dismiss);
        root.addView(zoomView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 关闭按钮
        TextView close = new TextView(ctx);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(22);
        close.setGravity(Gravity.CENTER);
        close.setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 40), UiKit.dp(ctx, 16), UiKit.dp(ctx, 16));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.END | Gravity.TOP;
        root.addView(close, clp);
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(root);
        dialog.show();
    }
}
