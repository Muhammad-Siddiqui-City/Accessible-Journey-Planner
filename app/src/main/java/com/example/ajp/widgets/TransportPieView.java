package com.example.ajp.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.ajp.R;

/**
 * Custom view implementation for TransportPieView.
 * Draws feature-specific visuals that are not covered by stock Android widgets.
 * Rendering concerns are isolated here so screens can pass data without drawing logic.
 */

public class TransportPieView extends View {

    private static final float[] ANGLES = {0.65f * 360f, 0.20f * 360f, 0.10f * 360f, 0.05f * 360f};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[] colors;

    public TransportPieView(Context context) {
        super(context);
        init(context);
    }

    public TransportPieView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TransportPieView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    private void init(Context context) {
        paint.setStyle(Paint.Style.FILL);
        colors = new int[]{
                ContextCompat.getColor(context, R.color.primary),
                ContextCompat.getColor(context, R.color.destructive),
                ContextCompat.getColor(context, R.color.line_overground),
                ContextCompat.getColor(context, R.color.line_elizabeth)
        };
    }

    @Override
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float size = Math.min(w, h);
        float pad = size * 0.15f;
        rect.set(pad, pad, size - pad, size - pad);
        float startAngle = 0;
        for (int i = 0; i < 4; i++) {
            paint.setColor(colors[i]);
            canvas.drawArc(rect, startAngle, ANGLES[i], true, paint);
            startAngle += ANGLES[i];
        }
        paint.setColor(ContextCompat.getColor(getContext(), R.color.card));
        float cx = rect.centerX();
        float cy = rect.centerY();
        float innerRadius = (rect.width() / 2f) * 0.5f;
        canvas.drawCircle(cx, cy, innerRadius, paint);
    }
}


