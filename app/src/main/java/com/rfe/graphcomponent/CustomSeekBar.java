package com.rfe.graphcomponent;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ProgressBar;

import java.util.Arrays;

public class CustomSeekBar extends ProgressBar {

    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar  The SeekBar whose progress has changed
         * @param progress The current progress level. This will be in the range 0..max where max
         *                 was set by {@link ProgressBar#setMax(int)}. (The default value for max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(CustomSeekBar seekBar, int progress, boolean fromUser);
    }

    private OnSeekBarChangeListener onSeekBarChangeListener;
    private int barWidth;
    private Paint paint = null;
    private final int BAR_HEIGHT = 2;
    private final int THUMB_HEIGHT = 75;
    private Point thumbPosition = new Point();
    private Rect barBounds = null;
    private Drawable thumb = null;
    private boolean isDragging = false;
    private int thumbOffset;
    private float scale;
    private int[] progressPoints;
    private boolean isCustomPointsMode = false;


    public CustomSeekBar(Context context) {
        super(context, null, android.R.attr.progressBarStyleHorizontal);

        setProgressDrawable(getResources().getDrawable(android.R.drawable.progress_horizontal));
        thumb = getResources().getDrawable(R.drawable.abc_btn_radio_material);
        setMax(1000);

        barBounds = new Rect();
        paint = new Paint();

        thumbOffset = THUMB_HEIGHT / 2;
        thumbPosition.x = THUMB_HEIGHT / 2;
        thumbPosition.y = THUMB_HEIGHT / 2;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setProgress(0);

        barBounds.left = getPaddingLeft() + THUMB_HEIGHT / 2;
        barBounds.right = getPaddingLeft() + getWidth() + getPaddingRight() - THUMB_HEIGHT / 2;
        barBounds.top = getPaddingTop() + THUMB_HEIGHT / 2;
        barBounds.bottom = barBounds.top + BAR_HEIGHT;

        barWidth = barBounds.right - barBounds.left;
        scale = ((float) barWidth) / getMax();

        Drawable progressDrawable = getProgressDrawable();

        progressDrawable.setBounds(barBounds.left, barBounds.top, barBounds.right, barBounds.bottom);
    }

    public int getThumbHeight() {
        return THUMB_HEIGHT;
    }

    public void setCustomPointsMode(boolean b) {
        isCustomPointsMode = b;
    }

    public void setProgressPoints(int[] a) {
        if (isCustomPointsMode)
            progressPoints = Arrays.copyOf(a, a.length);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLine(canvas);
        thumb.setBounds(thumbPosition.x - thumbOffset, thumbPosition.y - thumbOffset,
                thumbPosition.x + thumbOffset, thumbPosition.y + thumbOffset);
        thumb.draw(canvas);
    }

    private void drawLine(Canvas canvas) {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);
        canvas.drawLine(thumbPosition.x, thumbPosition.y,
                thumbPosition.x, getHeight() - getPaddingBottom(), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (touchTheThumb(event))
                    isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    setNewThumbPosition(event);
                    if (onSeekBarChangeListener != null && !isCustomPointsMode)
                        onSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isCustomPointsMode && isDragging) {
                    setFinalThumbPosition(event);
                    if (onSeekBarChangeListener != null)
                        onSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                }
                isDragging = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        onSeekBarChangeListener = l;
    }

    private void setFinalThumbPosition(MotionEvent event) {
        int progress = Math.round((event.getX() - barBounds.left) / scale);
        progress = findNearestPoint(progress);
        thumbPosition.x = barBounds.left + (int) (scale * progress);
        setProgress(progress);
    }

    private void setNewThumbPosition(MotionEvent event) {
        if (event.getX() < barBounds.right && event.getX() > barBounds.left) {
            thumbPosition.x = barBounds.left + (int) (Math.round((event.getX()
                    - barBounds.left) / scale) * scale);
            setProgress(Math.round((event.getX() - barBounds.left) / scale));
        } else if (event.getX() < barBounds.left) {
            thumbPosition.x = barBounds.left;
            setProgress(0);
        } else {
            thumbPosition.x = barBounds.right;
            setProgress(getMax());
        }
        invalidate();
    }

    private boolean touchTheThumb(MotionEvent event) {
        float deltaX = (event.getX() - thumbPosition.x);
        float deltaY = (event.getY() - thumbPosition.y);
        return deltaX * deltaX + deltaY * deltaY < thumbOffset * thumbOffset;
    }

    private int findNearestPoint(int key) {
        int lo = 0;
        int hi = progressPoints.length - 1;
        int point = progressPoints[0];
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;

            if (Math.abs(progressPoints[mid] - key) < Math.abs(key - point))
                point = progressPoints[mid];

            if (key < progressPoints[mid]) {
                hi = mid - 1;
            } else if (key > progressPoints[mid]) {
                lo = mid + 1;
            } else return point;
        }
        return point;
    }
}