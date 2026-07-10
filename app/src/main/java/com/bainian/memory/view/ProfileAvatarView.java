package com.bainian.memory.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.net.Uri;
import android.view.View;

/**
 * 「我的」页头部圆形头像 —— 未设置自定义头像时画简笔人物图标；
 * 一旦用户选择了照片，就用 BitmapShader 把照片裁成圆形铺满，替代默认图标。
 */
public class ProfileAvatarView extends View {
    private final Paint fgPaint;
    private final Paint bgPaint;
    private final Paint photoPaint;
    private Bitmap photoBitmap;

    public ProfileAvatarView(Context context) {
        super(context);
        fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fgPaint.setStyle(Paint.Style.STROKE);
        fgPaint.setStrokeCap(Paint.Cap.ROUND);
        fgPaint.setStrokeJoin(Paint.Join.ROUND);
        fgPaint.setColor(Color.WHITE);
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setSizeAndColor(float size, int color) {
        bgPaint.setColor(color);
        invalidate();
    }

    /** 传入一张头像图片的 Uri；传 null 则清空并回退到默认简笔头像 */
    public void setAvatarUri(Context ctx, String uriString) {
        photoBitmap = null;
        if (uriString != null && !uriString.isEmpty()) {
            try {
                Bitmap src = android.provider.MediaStore.Images.Media.getBitmap(
                        ctx.getContentResolver(), Uri.parse(uriString));
                photoBitmap = src;
            } catch (Exception ignored) {
                photoBitmap = null;
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f;

        if (photoBitmap != null) {
            drawPhoto(canvas, cx, cy, r);
            return;
        }

        // 背景圆
        canvas.drawCircle(cx, cy, r, bgPaint);

        // 人物图标线宽
        float sw = r * 0.08f;
        fgPaint.setStrokeWidth(sw);

        // 头部
        float headR = r * 0.26f;
        float headY = cy - r * 0.28f;
        canvas.drawCircle(cx, headY, headR, fgPaint);

        // 身体
        float bodyTop = headY + headR + sw * 2;
        Path path = new Path();
        path.moveTo(cx - r * 0.55f, cy + r * 0.6f);
        path.quadTo(cx - r * 0.45f, bodyTop, cx, bodyTop);
        path.quadTo(cx + r * 0.45f, bodyTop, cx + r * 0.55f, cy + r * 0.6f);
        canvas.drawPath(path, fgPaint);
    }

    private void drawPhoto(Canvas canvas, float cx, float cy, float r) {
        int bw = photoBitmap.getWidth(), bh = photoBitmap.getHeight();
        float scale = Math.max(getWidth() / (float) bw, getHeight() / (float) bh);

        BitmapShader shader = new BitmapShader(photoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate((getWidth() - bw * scale) / 2f, (getHeight() - bh * scale) / 2f);
        shader.setLocalMatrix(matrix);
        photoPaint.setShader(shader);

        canvas.drawCircle(cx, cy, r, photoPaint);
    }
}
