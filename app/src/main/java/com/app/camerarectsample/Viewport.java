package com.app.camerarectsample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

public class Viewport extends ViewGroup {

    public Viewport(Context context) {
        super(context);
    }

    public Viewport(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Viewport(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int viewportMargin = 32;
        int viewportCornerRadius = 2;

        int canvasW = getWidth();
        int canvasH = getHeight();
        Point centerOfCanvas = new Point(canvasW / 2, canvasH / 2);
        int rectH = canvasH / 3;
        int left = centerOfCanvas.x - (canvasW / 2);
        int top = centerOfCanvas.y - (rectH / 2);
        int right = centerOfCanvas.x + (canvasW / 2);
        int bottom = centerOfCanvas.y + (rectH / 2);

        Paint eraser = new Paint();
        eraser.setAntiAlias(true);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//        float width = (float) getWidth() - viewportMargin;
//        float height = width * (float) 0.7;
        RectF rect = new RectF(left, top, right, bottom);
        RectF frame = new RectF(left, top, right, bottom);
        Path path = new Path();
        Paint stroke = new Paint();
        stroke.setAntiAlias(true);
        stroke.setStrokeWidth(4);
        stroke.setColor(ContextCompat.getColor(getContext(), R.color.zantrik_color));
        stroke.setStyle(Paint.Style.STROKE);
        path.addRoundRect(frame, (float) viewportCornerRadius, (float) viewportCornerRadius, Path.Direction.CW);
        canvas.drawPath(path, stroke);
        canvas.drawRoundRect(rect, (float) viewportCornerRadius, (float) viewportCornerRadius, eraser);
    }
}