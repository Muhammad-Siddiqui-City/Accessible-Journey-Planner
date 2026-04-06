package com.example.ajp.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.ajp.R;

/** Simple bar chart for journeys-per-day on Analytics (tap highlights a bar). */
public class WeeklyBarChartView extends View {

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final String[] FULL_DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final int[] JOURNEYS = {4, 3, 5, 2, 4, 1, 0};

    private Paint barPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private int barColorBlue;
    private int barColorTeal;
    private int maxValue = 5;
    private float barWidth;
    private float chartTop;
    private float chartBottom;
    private float chartHeight;
    private int selectedIndex = -1;

    public interface OnBarSelectedListener {
        void onBarSelected(int index, String day, String fullDay, int journeys);
        void onBarDeselected();
    }

    private OnBarSelectedListener listener;

    public WeeklyBarChartView(Context context) {
        super(context);
        init(context);
    }

    public WeeklyBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeeklyBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.border));
        gridPaint.setStrokeWidth(1f);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(context, android.R.color.white));
        textPaint.setTextSize(dp(10));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.foreground));
        labelPaint.setTextSize(dp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        barColorBlue = ContextCompat.getColor(context, R.color.primary);
        barColorTeal = ContextCompat.getColor(context, R.color.secondary);
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    public void setOnBarSelectedListener(OnBarSelectedListener l) {
        listener = l;
    }

    @Override

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float paddingLeft = dp(24);
        float paddingRight = dp(16);
        float paddingTop = dp(8);
        float paddingBottom = dp(28);
        chartTop = paddingTop;
        chartBottom = h - paddingBottom;
        chartHeight = chartBottom - chartTop;

        int n = JOURNEYS.length;
        float totalBarArea = w - paddingLeft - paddingRight;
        barWidth = (totalBarArea / n) * 0.6f;
        float gap = (totalBarArea / n) * 0.4f;
        float barStart = paddingLeft + gap / 2;

        for (int i = 0; i < n; i++) {
            float x = barStart + i * (barWidth + gap) + barWidth / 2;
            int value = JOURNEYS[i];
            float barH = maxValue > 0 ? (value / (float) maxValue) * chartHeight : 0;
            if (value > 0 && barH < dp(20)) barH = dp(20);
            float top = chartBottom - barH;
            RectF rect = new RectF(
                    barStart + i * (barWidth + gap),
                    top,
                    barStart + i * (barWidth + gap) + barWidth,
                    chartBottom
            );
            boolean isHighest = value == maxValue && value > 0;
            barPaint.setColor(isHighest ? barColorTeal : barColorBlue);
            float radius = dp(4);
            canvas.drawRoundRect(rect, radius, radius, barPaint);
            if (value > 0) {
                canvas.drawText(String.valueOf(value), rect.centerX(), top + dp(12), textPaint);
            }
            canvas.drawText(DAYS[i], x, h - dp(8), labelPaint);
        }
    }

    @Override

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && listener != null) {
            float x = event.getX();
            float paddingLeft = dp(24);
            float paddingRight = dp(16);
            float totalBarArea = getWidth() - paddingLeft - paddingRight;
            int n = JOURNEYS.length;
            barWidth = (totalBarArea / n) * 0.6f;
            float gap = (totalBarArea / n) * 0.4f;
            float barStart = paddingLeft + gap / 2;
            for (int i = 0; i < n; i++) {
                float left = barStart + i * (barWidth + gap);
                if (x >= left && x <= left + barWidth) {
                    if (selectedIndex == i) {
                        selectedIndex = -1;
                        listener.onBarDeselected();
                    } else {
                        selectedIndex = i;
                        listener.onBarSelected(i, DAYS[i], FULL_DAYS[i], JOURNEYS[i]);
                    }
                    invalidate();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}

