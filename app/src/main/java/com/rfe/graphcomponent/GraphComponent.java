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

public class GraphComponent extends RelativeLayout{

    private float scaleX;
    private float scaleY;
    private int lineColor;
    private float gridStroke;
    private int barWidth;
    private int mLineColor = Color.GREEN;
    private int barHeight;
    private ArrayList<PointF> xy = null;
    private PointF[] cornerPoints = new PointF[2];


    public GraphComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.plot_layout, this, true);

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GraphComponent, 0, 0);

        mLineColor = a.getColor(
                R.styleable.GraphComponent_lineColor, mLineColor);

        a.recycle();
    }

    private void setScale(){
        scaleX = (((float) barWidth) - barHeight) / (cornerPoints[1].x - cornerPoints[0].x);
        scaleY = ((float) getHeight() - getPaddingTop() - getPaddingBottom() - 3 * barHeight)
                / (cornerPoints[0].y - cornerPoints[1].y);
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

        Log.i("Bar", Integer.toString(barHeight) + " " + Integer.toString(barWidth));
        if (xy == null)
            return;
        Log.i("onDraw", "OnDraw");

        setCornerPoints();
        setScale();


        Paint paint = new Paint();
        paint.setColor(mLineColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(3);

        PointF currentPoint = null;
        for(PointF p: xy) {
            if(currentPoint != null) {
                Log.i("Loop", "Loop");
                PointF nextPoint = transformPoint(p);
                Log.i("Loop", Float.toString(currentPoint.x) + " " + Float.toString(currentPoint.y));
                canvas.drawLine(currentPoint.x, currentPoint.y,
                        nextPoint.x, nextPoint.y, paint);
            }
            currentPoint = transformPoint(p);
        }
    }

    private PointF transformPoint(PointF p){
        int totalPaddingX = barHeight / 2 + getPaddingLeft();
        int totalPaddingY = getPaddingTop() + 2 * barHeight;
        return new PointF((p.x - cornerPoints[0].x) * scaleX + totalPaddingX,
                (cornerPoints[0].y - p.y) * scaleY + totalPaddingY);
    }

    public void setXY(ArrayList<PointF> xy){
        this.xy = new ArrayList<PointF>(xy);
        sortXY();
        Log.i("SetXY", "invalidate");
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
}