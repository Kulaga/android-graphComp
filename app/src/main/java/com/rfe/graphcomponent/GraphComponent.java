package com.rfe.graphcomponent;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GraphComponent extends RelativeLayout implements SeekBar.OnSeekBarChangeListener{

    private float mScaleX;
    private float mScaleY;
    private int mProgress = 0;
    private int mBarWidth;
    private Context mContext = null;
    private float mTextSize = 3;
    private int mStrokeColor = 0xFF007DFF;
    private int mAreaColor = 0x96007DFF;
    private int mTextColor = Color.WHITE;
    private int mGridColor = Color.WHITE;
    private float mStrokeWidth = 4;
    private float mLowerBound;
    private float mTickSize;
    private float mUpperBound;
    private float markerRadius = 5;
    private float mPointerX;
    private int mBarHeight;
    private ArrayList<PointF> mOriginalData = null;
    private PointF[] mCornerPoints = new PointF[2];


    public GraphComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.plot_layout, this, true);

        SeekBar seekBar = (SeekBar) findViewById(R.id.bar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(1000);

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GraphComponent, 0, 0);

        mStrokeColor = a.getColor(
                R.styleable.GraphComponent_strokeColor, mStrokeColor);
        mAreaColor = a.getColor(
                R.styleable.GraphComponent_areaColor, mAreaColor);
        mStrokeWidth = a.getFloat(
                R.styleable.GraphComponent_strokeWidth, mStrokeWidth);
        mTextColor = a.getColor(
                R.styleable.GraphComponent_textColor, mTextColor);
        mGridColor = a.getColor(
                R.styleable.GraphComponent_gridColor, mGridColor);
        a.recycle();

        mTextSize = dpToPixels(15);
        Log.i("Density", Float.toString(mContext.getResources().getDisplayMetrics().scaledDensity));
    }


    float dpToPixels(float sp) {
        return sp * mContext.getResources().getDisplayMetrics().scaledDensity;
    }

    private void setScale(){
        mScaleX = ((float) mBarWidth - mBarHeight) / (mCornerPoints[1].x - mCornerPoints[0].x);
        mScaleY = ((float) getHeight() - getPaddingTop() - getPaddingBottom() - 4 * mBarHeight)
                / (mUpperBound - mLowerBound);
    }

    private void setCornerPoints() {
        mCornerPoints[0] = new PointF(mOriginalData.get(0).x, mOriginalData.get(0).y);
        mCornerPoints[1] = new PointF(mOriginalData.get(mOriginalData.size() - 1).x, mOriginalData.get(0).y);
        for (PointF p : mOriginalData) {
            if(p.y > mCornerPoints[0].y)
                mCornerPoints[0].y = p.y;
            else if(p.y < mCornerPoints[1].y){
                mCornerPoints[1].y = p.y;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        SeekBar bar = (SeekBar) findViewById(R.id.bar);
        mBarWidth = bar.getWidth();
        mBarHeight = bar.getHeight();

        if (mOriginalData == null)
            return;

        setCornerPoints();
        if (mCornerPoints[0].x <0 || mCornerPoints[1].y  < 0)
            throw new NumberFormatException();
        setBounds();
        setScale();

        drawArea(canvas);
        drawPath(canvas);
        drawPointer(canvas);
        drawGrid(canvas);
        drawMarkers(canvas);
        drawValues(canvas);
    }

    private void drawValues(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(mTextColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(dpToPixels(1));
        paint.setTextSize(mTextSize);
        canvas.drawText("Speed: " + String.format("%.2f", findYatX(mPointerX)) + " km/h",
                (float) mBarHeight / 2, (float) 2 * mBarHeight + getPaddingTop(), paint);
    }

    private void setBounds(){
        double range = mCornerPoints[0].y - mCornerPoints[1].y;
        int tickCount = 4;
        double tickSize = range / (tickCount - 1);
        double a = Math.ceil(Math.log10(tickSize) - 1);
        double pow10a = Math.pow(10, a);
        float roundedTickSize = (float) (Math.ceil(tickSize / pow10a) * pow10a);

        mTickSize = roundedTickSize;
        mLowerBound = roundedTickSize * ((int)(mCornerPoints[1].y / roundedTickSize));
        mUpperBound = roundedTickSize * ((int)(1 + mCornerPoints[0].y / roundedTickSize));
        
    }

    private void drawGrid(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(mGridColor);
        paint.setAlpha(250);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dpToPixels(1));

        int i = 0;
        while(mLowerBound + mTickSize * i <= mUpperBound){
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
            canvas.drawTextOnPath(String.format("%.2f", currentY) + " km/h", path, 10, -10, paint);
            paint.setColor(mGridColor);
            i++;
        }
    }

    private int findSelectedMarker(){
        PointF crossPoint = transformPoint(new PointF(mPointerX, findYatX(mPointerX)));

        int i = 0;
        for(PointF p: mOriginalData){
            PointF p1 = transformPoint(p);
            double deltaX = p1.x - crossPoint.x;
            double deltaY = p1.y - crossPoint.y;
            float distance = (float) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

            if (distance <= 2 * markerRadius)
                return i;
            i++;
        }
        return -1;
    }

    private void drawMarkers(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(mStrokeColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(dpToPixels(1));

        int selectedMarker = findSelectedMarker();

        int i = 0;
        for(PointF p: mOriginalData){
            PointF transPoint = transformPoint(p);
            if (i == selectedMarker)
                canvas.drawCircle(transPoint.x, transPoint.y, 2 * markerRadius, paint);
            else
                canvas.drawCircle(transPoint.x, transPoint.y, markerRadius, paint);
            i++;
        }
    }

    private void drawPointer(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(mGridColor);
        paint.setStrokeWidth(dpToPixels(1));

        mPointerX = mCornerPoints[0].x + (mCornerPoints[1].x - mCornerPoints[0].x)
                * mProgress / 1000f;
        PointF p = transformPoint( new PointF(mPointerX, mLowerBound));
        PointF p1 = transformPoint( new PointF(mPointerX, mUpperBound));
        canvas.drawLine(p.x, p.y, p1.x, p1.y, paint);
    }

    private float findYatX(float x){
        PointF p1 = null;               // p1 < x < p
        for(PointF p: mOriginalData){
            if(p.x == x)
                return p.y;
            else if(p1 != null){
                if (p1.x < x && x < p.x)
                    return (x - p1.x) * (p.y - p1.y) / (p.x - p1.x) + p1.y;
            }
            p1 = p;
        }
        return 0;
    }

    private void drawPath(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(mStrokeColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dpToPixels(1));

        PointF currentPoint = null;
        for(PointF p: mOriginalData) {
            if(currentPoint != null) {
                PointF nextPoint = transformPoint(p);
                canvas.drawLine(currentPoint.x, currentPoint.y,
                        nextPoint.x, nextPoint.y, paint);
            }
            currentPoint = transformPoint(p);
        }
    }
    private void drawArea(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(mAreaColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);

        Path path = new Path();
        PointF begin = transformPoint(new PointF(mCornerPoints[0].x, mLowerBound));
        PointF end = transformPoint(new PointF(mCornerPoints[1].x, mLowerBound));
        path.moveTo(begin.x, begin.y);
        PointF transPoint;
        for(PointF p: mOriginalData) {
            transPoint = transformPoint(p);
            path.lineTo(transPoint.x, transPoint.y);
        }
        path.lineTo(end.x, end.y);
        path.close();
        canvas.drawPath(path, paint);

    }

    private PointF transformPoint(PointF p){
        int totalPaddingX = mBarHeight / 2 + getPaddingLeft();
        int totalPaddingY = getPaddingTop() + 3 * mBarHeight;

        return new PointF((p.x - mCornerPoints[0].x) * mScaleX + totalPaddingX,
                (mUpperBound - p.y) * mScaleY + totalPaddingY);
    }

    public void setOriginalData(ArrayList<PointF> mOriginalData){
        this.mOriginalData = new ArrayList<>(mOriginalData);
        sortOriginalData();
        postInvalidate();
    }

    private void sortOriginalData() {
        Collections.sort(mOriginalData, new Comparator<PointF>() {
            @Override
            public int compare(PointF p1, PointF p2) {
                if (p1.x < p2.x) {
                    return -1;
                }
                else if (p1.x > p2.x){
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        this.mProgress = progress;
        postInvalidate();

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}