package com.example.ajp.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.ajp.R;

/**
 * Custom view implementation for TimeSavedLineChartView.
 * Draws feature-specific visuals that are not covered by stock Android widgets.
 * Rendering concerns are isolated here so screens can pass data without drawing logic.
 */

public class TimeSavedLineChartView extends View {

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final String[] FULL_DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final int[] TIME_SAVED = {12, 8, 15, 5, 10, 3, 0};
    private static final int MAX_Y = 20;

    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint labelPaint;
    private Paint valuePaint;
    private int tealColor;
    private float[] pointsX;
    private float[] pointsY;
    private int selectedIndex = -1;

    public interface OnPointSelectedListener {
        void onPointSelected(int index, String day, String fullDay, int minutes);
        void onPointDeselected();
    }

    private OnPointSelectedListener listener;

    public TimeSavedLineChartView(Context context) {
        super(context);
        init(context);
    }

    public TimeSavedLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TimeSavedLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private void init(Context context) {
        tealColor = ContextCompat.getColor(context, R.color.secondary);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(tealColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(tealColor);
        fillPaint.setAlpha(40);
        fillPaint.setStyle(Paint.Style.FILL);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.border));
        gridPaint.setStrokeWidth(1f);
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.foreground));
        labelPaint.setTextSize(dp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(ContextCompat.getColor(context, R.color.muted_foreground));
        valuePaint.setTextSize(dp(9));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        pointsX = new float[7];
        pointsY = new float[7];
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    public void setOnPointSelectedListener(OnPointSelectedListener l) {
        listener = l;
    }

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        computePoints();
        int w = getWidth();
        int h = getHeight();
        float paddingBottom = dp(36);

        Path fillPath = new Path();
        fillPath.moveTo(pointsX[0], pointsY[0]);
        for (int i = 1; i < 7; i++) {
            fillPath.lineTo(pointsX[i], pointsY[i]);
        }
        fillPath.lineTo(pointsX[6], h - paddingBottom);
        fillPath.lineTo(pointsX[0], h - paddingBottom);
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);

        Path linePath = new Path();
        linePath.moveTo(pointsX[0], pointsY[0]);
        for (int i = 1; i < 7; i++) {
            linePath.lineTo(pointsX[i], pointsY[i]);
        }
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < 7; i++) {
            float radius = selectedIndex == i ? dp(8) : dp(5);
            linePaint.setStyle(Paint.Style.FILL);
            linePaint.setColor(tealColor);
            canvas.drawCircle(pointsX[i], pointsY[i], radius, linePaint);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setColor(0xFFFFFFFF);
            linePaint.setStrokeWidth(dp(2));
            canvas.drawCircle(pointsX[i], pointsY[i], radius, linePaint);
            linePaint.setColor(tealColor);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(dp(2.5f));
        }

        for (int i = 0; i < 7; i++) {
            canvas.drawText(DAYS[i], pointsX[i], h - dp(22), labelPaint);
            canvas.drawText(TIME_SAVED[i] > 0 ? TIME_SAVED[i] + "m" : "–", pointsX[i], h - dp(10), valuePaint);
        }
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private void computePoints() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float paddingLeft = dp(28);
        float paddingRight = dp(16);
        float paddingTop = dp(8);
        float paddingBottom = dp(36);
        float chartW = w - paddingLeft - paddingRight;
        float chartH = h - paddingTop - paddingBottom;
        for (int i = 0; i < 7; i++) {
            pointsX[i] = paddingLeft + (i / 6f) * chartW;
            float ratio = MAX_Y > 0 ? (TIME_SAVED[i] / (float) MAX_Y) : 0;
            pointsY[i] = paddingTop + (1 - ratio) * chartH;
        }
    }

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && listener != null) {
            computePoints();
            float x = event.getX();
            float y = event.getY();
            float hit = dp(24);
            for (int i = 0; i < 7; i++) {
                if (Math.abs(x - pointsX[i]) <= hit && Math.abs(y - pointsY[i]) <= hit) {
                    if (selectedIndex == i) {
                        selectedIndex = -1;
                        listener.onPointDeselected();
                    } else {
                        selectedIndex = i;
                        listener.onPointSelected(i, DAYS[i], FULL_DAYS[i], TIME_SAVED[i]);
                    }
                    invalidate();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}


