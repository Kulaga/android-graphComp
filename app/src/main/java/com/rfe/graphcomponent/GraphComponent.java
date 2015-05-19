package com.rfe.graphcomponent;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
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

    private float scaleX;
    private float scaleY;
    private int progress = 0;
    private int lineColor;
    private float gridStroke;
    private int barWidth;
    private int mLineColor = Color.GREEN;
    private float mLowerBound;
    private float mTickSize;
    private float mUpperBound;
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
        scaleX = (((float) barWidth) - barHeight) / (cornerPoints[1].x - cornerPoints[0].x);
        scaleY = ((float) getHeight() - getPaddingTop() - getPaddingBottom() - 3 * barHeight)
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
        drawGrid(canvas);
        drawNormalLine(canvas);
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
        for(int i=0; i < 4; i++){
            Path path = new Path();
            float currentY = mLowerBound + mTickSize * i;
            PointF p = transformPoint(new PointF(cornerPoints[0].x, currentY));
            PointF p1 = transformPoint(new PointF(cornerPoints[1].x, currentY));

            path.moveTo(p.x, p.y);
            path.lineTo(p1.x, p1.y);
            canvas.drawPath(path, paint);
            canvas.drawTextOnPath(String.format("%.2f", currentY) + " km/h", path, 10, -10, paint);
        }
    }

    private void drawNormalLine(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);

        float x = cornerPoints[0].x + (cornerPoints[1].x - cornerPoints[0].x)
                * progress / 100;
        PointF p = transformPoint( new PointF(x, mLowerBound));
        PointF p1 = transformPoint( new PointF(x, mUpperBound));
        canvas.drawLine(p.x, p.y, p1.x, p1.y, paint);
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
        int totalPaddingY = getPaddingTop() +  2 * barHeight;

        return new PointF((p.x - cornerPoints[0].x) * scaleX + totalPaddingX,
                (mUpperBound - p.y) * scaleY + totalPaddingY);
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
        this.progress = progress;
        postInvalidate();

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}