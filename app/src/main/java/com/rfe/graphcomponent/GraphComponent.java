package com.rfe.graphcomponent;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class GraphComponent extends RelativeLayout implements SeekBar.OnSeekBarChangeListener{

    private float mScaleX;
    private float mScaleY;
    private int mProgress = 0;
    private int lineColor;
    private float gridStroke;
    private int barWidth;
    private int mLineColor = Color.GREEN;
    private float mLowerBound;
    private float mTickSize;
    private float mUpperBound;
    private float markerRadius = 5;
    private int mSelectedMarker = -1;
    private float mPointerX;
    private int barHeight;
    private ArrayList<PointF> xy = null;
    private PointF[] cornerPoints = new PointF[2];


    public GraphComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.plot_layout, this, true);

        SeekBar seekBar = (SeekBar) findViewById(R.id.bar);
        seekBar.setOnSeekBarChangeListener(this);

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GraphComponent, 0, 0);

        mLineColor = a.getColor(
                R.styleable.GraphComponent_lineColor, mLineColor);

        a.recycle();
    }

    private void setScale(){
        mScaleX = ((float) barWidth - barHeight) / (cornerPoints[1].x - cornerPoints[0].x);
        mScaleY = ((float) getHeight() - getPaddingTop() - getPaddingBottom() - 4 * barHeight)
                / (mUpperBound - mLowerBound);
    }

    private void setCornerPoints() {
        cornerPoints[0] = new PointF(xy.get(0).x, xy.get(0).y);
        cornerPoints[1] = new PointF(xy.get(xy.size() - 1).x, xy.get(0).y);
        for (PointF p : xy) {
            if(p.y > cornerPoints[0].y)
                cornerPoints[0].y = p.y;
            else if(p.y < cornerPoints[1].y){
                cornerPoints[1].y = p.y;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        SeekBar bar = (SeekBar) findViewById(R.id.bar);
        barWidth = bar.getWidth();
        barHeight = bar.getHeight();

        if (xy == null)
            return;

        setCornerPoints();
        if (cornerPoints[0].x <0 || cornerPoints[1].y  < 0)
            throw new NumberFormatException();

        setBounds();
        setScale();
        drawArea(canvas);
        drawPath(canvas);
        drawNormalLine(canvas);
        drawGrid(canvas);
        drawMarkers(canvas);
        drawValues(canvas);
    }

    private void drawValues(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextSize(30);
        canvas.drawText("Speed: " + String.format("%.2f",findYatX(mPointerX)) + " km/h",
                (float)barHeight / 2, (float) 2 * barHeight, paint);
    }

    private void setBounds(){
        double range = cornerPoints[0].y - cornerPoints[1].y;
        int tickCount = 4;
        double tickSize = range / (tickCount - 1);
        double a = Math.ceil(Math.log10(tickSize) - 1);
        double pow10a = Math.pow(10, a);
        float roundedTickSize = (float) (Math.ceil(tickSize / pow10a) * pow10a);

        mTickSize = roundedTickSize;
        mLowerBound = roundedTickSize * ((int)(cornerPoints[1].y / roundedTickSize));
        mUpperBound = roundedTickSize * ((int)(1 + cornerPoints[0].y / roundedTickSize));
        
    }

    private void drawGrid(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAlpha(250);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);

        int ticks = 4;
        if(mUpperBound > mTickSize * ticks)
            ticks++;

        int i = 0;
        while(mLowerBound + mTickSize * i <= mUpperBound){
            Path path = new Path();
            float currentY = mLowerBound + mTickSize * i;
            PointF p = transformPoint(new PointF(cornerPoints[0].x, currentY));
            PointF p1 = transformPoint(new PointF(cornerPoints[1].x, currentY));

            path.moveTo(p.x, p.y);
            path.lineTo(p1.x, p1.y);
            canvas.drawPath(path, paint);
            canvas.drawTextOnPath(String.format("%.2f", currentY) + " km/h", path, 10, -10, paint);
            i++;
        }
    }

    private int findSelectedMarker(){
        PointF crossPoint = transformPoint(new PointF(mPointerX, findYatX(mPointerX)));

        int i = 0;
        for(PointF p: xy){
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
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);

        int selectedMarker = findSelectedMarker();

        int i = 0;
        for(PointF p: xy){
            PointF transPoint = transformPoint(p);
            if (i == selectedMarker)
                canvas.drawCircle(transPoint.x, transPoint.y, 2 * markerRadius, paint);
            else
                canvas.drawCircle(transPoint.x, transPoint.y, markerRadius, paint);
            i++;
        }
    }

    private void drawNormalLine(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1);

        mPointerX = cornerPoints[0].x + (cornerPoints[1].x - cornerPoints[0].x)
                * mProgress / 100f;
        PointF p = transformPoint( new PointF(mPointerX, mLowerBound));
        PointF p1 = transformPoint( new PointF(mPointerX, mUpperBound));
        canvas.drawLine(p.x, p.y, p1.x, p1.y, paint);
    }

    private float findYatX(float x){
        PointF p1 = null, p2 = null;                             // p3.x < x < p4.x
        for(PointF p: xy){
            if(p.x == x)
                return p.y;
            if(p1 != null){
                if (p1.x < x && x < p.x){
                    p2 = p;
                    break;
                }
            }
            p1 = p;
        }
        return (x - p1.x) * (p2.y - p1.y) / (p2.x - p1.x) + p1.y;
    }

    private void drawPath(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(mLineColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        PointF currentPoint = null;
        for(PointF p: xy) {
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
        paint.setColor(mLineColor);
        paint.setAlpha(150);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);

        Path path = new Path();
        PointF begin = transformPoint(new PointF(cornerPoints[0].x, mLowerBound));
        PointF end = transformPoint(new PointF(cornerPoints[1].x, mLowerBound));
        path.moveTo(begin.x, begin.y);
        PointF transPoint;
        for(PointF p: xy) {
            transPoint = transformPoint(p);
            path.lineTo(transPoint.x, transPoint.y);
        }
        path.lineTo(end.x, end.y);
        path.close();
        canvas.drawPath(path, paint);

    }

    private PointF transformPoint(PointF p){
        int totalPaddingX = barHeight / 2 + getPaddingLeft();
        int totalPaddingY = getPaddingTop() +  3 * barHeight;

        return new PointF((p.x - cornerPoints[0].x) * mScaleX + totalPaddingX,
                (mUpperBound - p.y) * mScaleY + totalPaddingY);
    }

    public void setXY(ArrayList<PointF> xy){
        this.xy = new ArrayList<>(xy);
        sortXY();
        postInvalidate();
    }

    private void sortXY() {
        Collections.sort(xy, new Comparator<PointF>() {
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