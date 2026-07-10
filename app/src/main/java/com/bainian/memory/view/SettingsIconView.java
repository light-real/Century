package com.bainian.memory.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * 全局共用的矢量小图标 View——最初只用于「我的」设置列表（蛋糕/信息圆圈/齿轮），
 * 现在承载起全 App 通用的手绘几何图标库：日历、相机、勾选、关闭、星星等，
 * 只要是散落在各页面里、之前用 emoji 或纯文本符号顶替的小图标，都统一收敛到这里绘制，
 * 保证描边宽度、圆角线帽这套「同一种字体」的视觉语言不因为页面不同而走样。
 */
public class SettingsIconView extends View {

    /** 图标类型 */
    public enum Kind { BIRTH, ABOUT, SYSTEM, CAMERA, CALENDAR, CHECK, CLOSE, SPARK }

    private Kind kind = Kind.BIRTH;
    private int iconColor = 0xFF5F6368;
    private final Paint paint;
    private final Path path;

    public SettingsIconView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        path = new Path();
    }

    public void setKind(Kind k) { kind = k; invalidate(); }
    public void setIconColor(int c) { iconColor = c; paint.setColor(c); invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float m = w * 0.18f;
        paint.setColor(iconColor);
        path.reset();

        switch (kind) {
            case BIRTH: // 蛋糕
                paint.setStrokeWidth(w * 0.085f);
                float cakeTop = h * 0.28f;
                float cakeBot = h * 0.72f;
                // 蜡烛
                float cMx = w / 2f;
                canvas.drawLine(cMx, h * 0.12f, cMx, cakeTop + w * 0.02f, paint);
                // 火焰
                paint.setStyle(Paint.Style.FILL);
                float flameY = h * 0.08f;
                path.moveTo(cMx - w * 0.1f, flameY + w * 0.08f);
                path.quadTo(cMx - w * 0.06f, flameY, cMx, flameY);
                path.quadTo(cMx + w * 0.06f, flameY, cMx + w * 0.1f, flameY + w * 0.08f);
                path.quadTo(cMx + w * 0.04f, flameY + w * 0.05f, cMx, flameY + w * 0.1f);
                path.quadTo(cMx - w * 0.04f, flameY + w * 0.05f, cMx - w * 0.1f, flameY + w * 0.08f);
                path.close();
                canvas.drawPath(path, paint);
                paint.setStyle(Paint.Style.STROKE);
                // 蛋糕体
                canvas.drawRoundRect(m, cakeTop, w - m, cakeBot, w * 0.06f, w * 0.06f, paint);
                // 中间横线
                canvas.drawLine(m + w * 0.1f, cakeTop + (cakeBot - cakeTop) * 0.4f,
                        w - m - w * 0.1f, cakeTop + (cakeBot - cakeTop) * 0.4f, paint);
                break;

            case ABOUT: // 信息圆圈
                paint.setStrokeWidth(w * 0.09f);
                float cr = w * 0.33f;
                canvas.drawCircle(w / 2f, h / 2f, cr, paint);
                // 竖线
                canvas.drawLine(w / 2f, h / 2f - cr * 0.45f,
                        w / 2f, h / 2f - cr * 0.2f, paint);
                // 小点
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(w / 2f, h / 2f + cr * 0.3f, w * 0.06f, paint);
                paint.setStyle(Paint.Style.STROKE);
                break;

            case SYSTEM: // 齿轮
                paint.setStrokeWidth(w * 0.07f);
                float gr = w * 0.28f;
                float gcx = w / 2f, gcy = h / 2f;
                canvas.drawCircle(gcx, gcy, gr, paint);
                // 锯齿
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60);
                    float ix = (float) (gcx + (gr + w * 0.06f) * Math.cos(angle));
                    float iy = (float) (gcy + (gr + w * 0.06f) * Math.sin(angle));
                    float ox = (float) (gcx + (gr - w * 0.10f) * Math.cos(angle));
                    float oy = (float) (gcy + (gr - w * 0.10f) * Math.sin(angle));
                    canvas.drawLine(ix, iy, ox, oy, paint);
                }
                // 中心小圆
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(gcx, gcy, w * 0.08f, paint);
                paint.setStyle(Paint.Style.STROKE);
                break;

            case CAMERA: // 相机——用于头像上的「点击更换」角标，替代生硬的 emoji
                paint.setStrokeWidth(w * 0.09f);
                float bodyTop = h * 0.34f;
                float bodyBot = h * 0.78f;
                canvas.drawRoundRect(w * 0.14f, bodyTop, w * 0.86f, bodyBot, w * 0.08f, w * 0.08f, paint);
                // 顶部取景凸起
                canvas.drawLine(w * 0.38f, bodyTop, w * 0.42f, h * 0.22f, paint);
                canvas.drawLine(w * 0.42f, h * 0.22f, w * 0.62f, h * 0.22f, paint);
                canvas.drawLine(w * 0.62f, h * 0.22f, w * 0.66f, bodyTop, paint);
                // 镜头圆圈
                canvas.drawCircle(w / 2f, (bodyTop + bodyBot) / 2f, w * 0.15f, paint);
                break;

            case CALENDAR: // 日历——首页顶部次级入口，替代 emoji 🗓
                paint.setStrokeWidth(w * 0.085f);
                float calTop = h * 0.24f;
                float calBot = h * 0.84f;
                canvas.drawRoundRect(w * 0.14f, calTop, w * 0.86f, calBot, w * 0.06f, w * 0.06f, paint);
                // 顶部装订环
                canvas.drawLine(w * 0.32f, calTop, w * 0.32f, h * 0.14f, paint);
                canvas.drawLine(w * 0.68f, calTop, w * 0.68f, h * 0.14f, paint);
                // 顶部横条分隔「表头」
                canvas.drawLine(w * 0.14f, calTop + (calBot - calTop) * 0.26f,
                        w * 0.86f, calTop + (calBot - calTop) * 0.26f, paint);
                // 一颗代表「日期」的小圆点
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(w * 0.5f, calTop + (calBot - calTop) * 0.62f, w * 0.055f, paint);
                paint.setStyle(Paint.Style.STROKE);
                break;

            case CHECK: // 勾选——完成态的对勾，替代文本符号 ✓
                paint.setStrokeWidth(w * 0.14f);
                path.moveTo(w * 0.22f, h * 0.52f);
                path.lineTo(w * 0.42f, h * 0.72f);
                path.lineTo(w * 0.8f, h * 0.3f);
                canvas.drawPath(path, paint);
                break;

            case CLOSE: // 关闭/删除——替代文本符号 ✕
                paint.setStrokeWidth(w * 0.11f);
                canvas.drawLine(w * 0.24f, h * 0.24f, w * 0.76f, h * 0.76f, paint);
                canvas.drawLine(w * 0.76f, h * 0.24f, w * 0.24f, h * 0.76f, paint);
                break;

            case SPARK: // 星火——「那年今日」怀旧标记，替代 emoji ✨
                paint.setStyle(Paint.Style.FILL);
                float scx = w / 2f, scy = h / 2f;
                float big = w * 0.34f, small = w * 0.16f;
                drawSpark(canvas, scx, scy, big);
                drawSpark(canvas, w * 0.22f, h * 0.28f, small);
                paint.setStyle(Paint.Style.STROKE);
                break;
        }
    }

    /** 画一个四角星（简易星芒），用于 SPARK 图标的大小两颗星 */
    private void drawSpark(Canvas canvas, float cx, float cy, float r) {
        path.reset();
        path.moveTo(cx, cy - r);
        path.quadTo(cx + r * 0.18f, cy - r * 0.18f, cx + r, cy);
        path.quadTo(cx + r * 0.18f, cy + r * 0.18f, cx, cy + r);
        path.quadTo(cx - r * 0.18f, cy + r * 0.18f, cx - r, cy);
        path.quadTo(cx - r * 0.18f, cy - r * 0.18f, cx, cy - r);
        path.close();
        canvas.drawPath(path, paint);
    }
}
