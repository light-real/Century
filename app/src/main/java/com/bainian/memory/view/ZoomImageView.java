package com.bainian.memory.view;

import android.content.Context;
import android.graphics.Canvas;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** 支持双指缩放的 ImageView，用于全屏图片查看；单击（非缩放手势）会回调 onTapListener，通常用于退出全屏 */
public class ZoomImageView extends ImageView {
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector tapDetector;
    private float scale = 1f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 5f;

    private Runnable onTapListener;

    public ZoomImageView(Context context) {
        super(context);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        tapDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (onTapListener != null) onTapListener.run();
                return true;
            }
        });
    }

    /** 单击图片（非缩放手势）时触发，用于外部实现「再次点击退出全屏」 */
    public void setOnTapListener(Runnable listener) {
        this.onTapListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        tapDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.scale(scale, scale, getWidth() / 2f, getHeight() / 2f);
        super.onDraw(canvas);
        canvas.restore();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
            invalidate();
            return true;
        }
    }
}
