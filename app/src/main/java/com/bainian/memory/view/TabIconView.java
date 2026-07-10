package com.bainian.memory.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * 底部导航自定义图标——三个图标统一线条粗细、统一圆角风格，
 * 让「回忆 / 时间线 / 计划」在视觉上是「同一套语言」而不是拼凑的几何图形。
 * 统一规则：描边宽度 s*0.105，线帽/线接圆角，主体收在同一个安全区内。
 */
public class TabIconView extends View {
    private int kind;
    private int iconColor = 0xFF5F6368;
    private float iconSize;
    private final Paint paint;
    private final Paint fillPaint;
    private final Path path;

    public TabIconView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        path = new Path();
    }

    public void setKind(int k) { kind = k; invalidate(); }
    public void setSize(float s) { iconSize = s; invalidate(); }
    public void setColor(int c) { iconColor = c; paint.setColor(c); fillPaint.setColor(c); invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float m = w * 0.16f;
        float s = iconSize > 0 ? iconSize : w;
        float sw = s * 0.105f;

        paint.setColor(iconColor);
        fillPaint.setColor(iconColor);
        paint.setStrokeWidth(sw);
        path.reset();

        switch (kind) {
            case 0: { // 回忆 —— 圆润心形，情感化的回忆入口
                float hcx = w / 2f, hTop = m + s * 0.05f;
                float hw = (w - 2 * m) / 2f;
                path.moveTo(hcx, h - m);
                path.cubicTo(hcx - hw * 1.28f, h - m - hw * 1.1f,
                        hcx - hw, hTop,
                        hcx, hTop + hw * 0.58f);
                path.cubicTo(hcx + hw, hTop,
                        hcx + hw * 1.28f, h - m - hw * 1.1f,
                        hcx, h - m);
                canvas.drawPath(path, paint);
                break;
            }

            case 1: { // 时间线 —— 一条主轴 + 三个圆点锚点，呼应「回看走过的路」
                float cx = w / 2f;
                float top = m + s * 0.02f, bottom = h - m - s * 0.02f;
                canvas.drawLine(cx, top, cx, bottom, paint);
                float r = sw * 0.95f;
                float[] ys = {top, (top + bottom) / 2f, bottom};
                for (float y : ys) {
                    canvas.drawCircle(cx, y, r, fillPaint);
                }
                break;
            }

            case 2: { // 计划 —— 一枚打勾的圆角方框，代表「划掉一件事」的完成感
                float boxSize = h - 2 * m;
                float left = (w - boxSize) / 2f;
                float top = m;
                canvas.drawRoundRect(left, top, left + boxSize, top + boxSize,
                        boxSize * 0.32f, boxSize * 0.32f, paint);
                path.reset();
                path.moveTo(left + boxSize * 0.24f, top + boxSize * 0.53f);
                path.lineTo(left + boxSize * 0.44f, top + boxSize * 0.74f);
                path.lineTo(left + boxSize * 0.78f, top + boxSize * 0.28f);
                canvas.drawPath(path, paint);
                break;
            }
        }
    }
}
