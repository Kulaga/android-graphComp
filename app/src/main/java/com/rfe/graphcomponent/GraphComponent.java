package com.rfe.graphcomponent;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GraphComponent extends RelativeLayout implements CustomSeekBar.OnSeekBarChangeListener {

    private float mScaleX;
    private float mScaleY;
    private int mProgress = 0;
    private Context mContext = null;
    private int mTextSize;
    private int mStrokeColor = 0xFF007DFF;
    private int mAreaColor = 0x96007DFF;
    private int mTextColor = Color.WHITE;
    private int mGridColor = Color.WHITE;
    private int mFlagColor = 0xc8c9c538;
    private float mStrokeWidth = 3;
    private float mLowerBound;
    private float mTickSize;
    private float mUpperBound;
    private final float markerRadius = 5;
    private int mBarWidth;
    private int mBarHeight;
    Paint paint = new Paint();
    private ArrayList<PointF> mOriginalData = null;
    private PointF[] mCornerPoints = new PointF[2];
    private int mRangeOfProgress = 1000;
    private CustomSeekBar mCustomSeekBar = null;


    public GraphComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.plot_layout, this, true);

        mCustomSeekBar = (CustomSeekBar) findViewById(R.id.customSeekBar);
        mCustomSeekBar.setOnSeekBarChangeListener(this);
        mCustomSeekBar.setMax(1000);
        mCustomSeekBar.setCustomPointsMode(true);
        mBarHeight = mCustomSeekBar.getThumbHeight();

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GraphComponent, 0, 0);

        mStrokeColor = a.getColor(
                R.styleable.GraphComponent_strokeColor, mStrokeColor);
        mAreaColor = a.getColor(
                R.styleable.GraphComponent_areaColor, mAreaColor);
        mTextColor = a.getColor(
                R.styleable.GraphComponent_textColor, mTextColor);
        mGridColor = a.getColor(
                R.styleable.GraphComponent_gridColor, mGridColor);
        mFlagColor = a.getColor(
                R.styleable.GraphComponent_flagColor, mFlagColor);
        a.recycle();
    }

    private void setProgressPoints() {
        int[] a = new int[mOriginalData.size()];
        for (int i = 0; i < a.length; i++) {
            int progress = (int) ((mOriginalData.get(i).x - mCornerPoints[0].x) /
                    (mCornerPoints[1].x - mCornerPoints[0].x) * mRangeOfProgress);
            a[i] = progress;
        }
        mCustomSeekBar.setProgressPoints(a);
    }

    float dpToPixels(float sp) {
        return sp * mContext.getResources().getDisplayMetrics().scaledDensity;
    }

    private void setScale() {
        int flagHeight = 3 * mTextSize;
        mScaleX = ((float) mBarWidth - mBarHeight) / (mCornerPoints[1].x - mCornerPoints[0].x);
        mScaleY = ((float) getHeight() - getPaddingTop() - getPaddingBottom() - mBarHeight -
                flagHeight) / (mUpperBound - mLowerBound);
    }

    private void setCornerPoints() {
        mCornerPoints[0] = new PointF(mOriginalData.get(0).x, mOriginalData.get(0).y);
        mCornerPoints[1] = new PointF(mOriginalData.get(mOriginalData.size() - 1).x, mOriginalData.get(0).y);
        for (PointF p : mOriginalData) {
            if (p.y > mCornerPoints[0].y)
                mCornerPoints[0].y = p.y;
            else if (p.y < mCornerPoints[1].y) {
                mCornerPoints[1].y = p.y;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBarWidth = w;
        mTextSize = w < h ? w / 20 : h / 20;
        setScale();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mOriginalData == null)
            return;

        drawArea(canvas);
        drawPath(canvas);
        drawMarkers(canvas);
        drawGrid(canvas);
        drawFlag(canvas);
    }

    private void drawFlag(Canvas canvas) {
        paint.setStrokeWidth(dpToPixels(2));
        for (PointF p : mOriginalData) {
            if (isSelectedPoint(p)) {
                drawFlag(p, String.format("%.0f", p.y) + " km/h", canvas);
            }
        }
    }

    private Path getFlagPath(PointF p, float h1, float h2, float w) {
        Path path = new Path();

        PointF startPoint = transformPoint(p);
        path.moveTo(startPoint.x, startPoint.y);
        path.lineTo(startPoint.x, startPoint.y - h2);

        if (startPoint.x + w > getWidth())
            w = -w;
        path.lineTo(startPoint.x + w, startPoint.y - h2);
        path.lineTo(startPoint.x + w, startPoint.y - h1);
        path.lineTo(startPoint.x, startPoint.y - h1);
        path.close();
        return path;
    }

    private void drawFlag(PointF p, String text, Canvas canvas) {
        paint.setTextSize(mTextSize);

        float h = (paint.descent() - paint.ascent()) * 3;
        float w = paint.measureText(text) * 1.5f;

        Path flag = getFlagPath(p, h / 3, h, w);

        paint.setColor(mFlagColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        canvas.drawPath(flag, paint);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(flag, paint);

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(2);
        PointF p1 = transformPoint(p);
        w = p1.x + w < getWidth() ? w / 6 : -w * 5 / 6;

        paint.setAntiAlias(true);
        canvas.drawText(text, p1.x + w, p1.y - h * 0.5f, paint);
    }

    private void setBounds() {
        double range = mCornerPoints[0].y - mCornerPoints[1].y;
        int tickCount = 4;
        double tickSize = range / (tickCount - 1);
        double a = Math.ceil(Math.log10(tickSize) - 1);
        double pow10a = Math.pow(10, a);
        float roundedTickSize = (float) (Math.ceil(tickSize / pow10a) * pow10a);

        mTickSize = roundedTickSize;
        mLowerBound = roundedTickSize * ((int) (mCornerPoints[1].y / roundedTickSize));
        mUpperBound = roundedTickSize * ((int) (1 + mCornerPoints[0].y / roundedTickSize));
    }

    private void drawGrid(Canvas canvas) {
        paint.setColor(mGridColor);
        paint.setAlpha(250);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dpToPixels(1));

        int i = 0;
        while (mLowerBound + mTickSize * i <= mUpperBound) {
            Path path = new Path();
            float currentY = mLowerBound + mTickSize * i;
            PointF p = transformPoint(new PointF(mCornerPoints[0].x, currentY));
            PointF p1 = transformPoint(new PointF(mCornerPoints[1].x, currentY));

            path.moveTo(p.x, p.y);
            path.lineTo(p1.x, p1.y);
            canvas.drawPath(path, paint);

            paint.setColor(mTextColor);
            paint.setTextSize(mTextSize);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setAntiAlias(true);
            canvas.drawTextOnPath(String.format("%.0f", currentY) + " km/h", path, 10, -10, paint);

            paint.setAntiAlias(false);
            paint.setColor(mGridColor);
            i++;
        }
    }

    private boolean isSelectedPoint(PointF p) {
        int progress = (int) ((p.x - mCornerPoints[0].x) /
                (mCornerPoints[1].x - mCornerPoints[0].x) * mRangeOfProgress);
        return progress == mProgress;
    }

    private void drawMarkers(Canvas canvas) {
        paint.setColor(mStrokeColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(dpToPixels(1));

        for (PointF p : mOriginalData) {
            PointF p1 = transformPoint(p);
            if (isSelectedPoint(p))
                canvas.drawCircle(p1.x, p1.y, 2 * dpToPixels(markerRadius), paint);
            else
                canvas.drawCircle(p1.x, p1.y, dpToPixels(markerRadius), paint);
        }
    }

    private void drawPath(Canvas canvas) {
        paint.setColor(mStrokeColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dpToPixels(mStrokeWidth));


        PointF currentPoint = null;
        for (PointF p : mOriginalData) {
            if (currentPoint != null) {
                PointF nextPoint = transformPoint(p);
                canvas.drawLine(currentPoint.x, currentPoint.y,
                        nextPoint.x, nextPoint.y, paint);
            }
            currentPoint = transformPoint(p);
        }
    }

    private void drawArea(Canvas canvas) {
        paint.setColor(mAreaColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);

        Path path = new Path();
        PointF begin = transformPoint(new PointF(mCornerPoints[0].x, mLowerBound));
        PointF end = transformPoint(new PointF(mCornerPoints[1].x, mLowerBound));
        path.moveTo(begin.x, begin.y);
        PointF transPoint;
        for (PointF p : mOriginalData) {
            transPoint = transformPoint(p);
            path.lineTo(transPoint.x, transPoint.y);
        }
        path.lineTo(end.x, end.y);
        path.close();
        canvas.drawPath(path, paint);

    }

    private PointF transformPoint(PointF p) {
        int flagHeight = 3 * mTextSize;
        int totalPaddingX = mBarHeight / 2 + getPaddingLeft();
        int totalPaddingY = getPaddingTop() + flagHeight + mBarHeight;

        return new PointF((p.x - mCornerPoints[0].x) * mScaleX + totalPaddingX,
                (mUpperBound - p.y) * mScaleY + totalPaddingY);
    }

    public void setOriginalData(ArrayList<PointF> mOriginalData) {
        this.mOriginalData = new ArrayList<>(mOriginalData);
        sortOriginalData();
        setCornerPoints();
        setProgressPoints();

        if (mCornerPoints[0].x < 0 || mCornerPoints[1].y < 0)
            throw new NumberFormatException();
        setBounds();
        invalidate();
    }

    private void sortOriginalData() {
        Collections.sort(mOriginalData, new Comparator<PointF>() {
            @Override
            public int compare(PointF p1, PointF p2) {
                if (p1.x < p2.x) {
                    return -1;
                } else if (p1.x > p2.x) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    @Override
    public void onProgressChanged(CustomSeekBar seekBar, int progress, boolean fromUser) {
        this.mProgress = progress;
        invalidate();
    }
}