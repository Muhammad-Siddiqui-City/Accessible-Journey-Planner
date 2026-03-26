package com.example.ajp.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.ajp.R;




/**
 * Custom view for rendering RoutePreview content.
 */
public class RoutePreviewView extends View {

    private Paint pathPaint;
    private Paint gridPaint;
    private Paint circlePaint;
    private int tealColor;
    private int blueColor;
    private int purpleColor;
    private int redColor;

    public RoutePreviewView(Context context) {
        super(context);
        init(context);
    }

    public RoutePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RoutePreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        tealColor = ContextCompat.getColor(context, R.color.secondary);
        blueColor = ContextCompat.getColor(context, R.color.primary);
        purpleColor = ContextCompat.getColor(context, R.color.line_elizabeth);
        redColor = ContextCompat.getColor(context, R.color.destructive);
        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(dp(4));
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0x330055FF);
        gridPaint.setStrokeWidth(1f);
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(dp(2));
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float gridSize = dp(20);
        for (float x = 0; x <= w; x += gridSize) {
            canvas.drawLine(x, 0, x, h, gridPaint);
        }
        for (float y = 0; y <= h; y += gridSize) {
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        float scaleX = w / 100f;
        float scaleY = h / 100f;
        Path path = new Path();
        path.moveTo(15 * scaleX, 78 * scaleY);
        path.cubicTo(15 * scaleX, 55 * scaleY, 15 * scaleX, 45 * scaleY, 28 * scaleX, 45 * scaleY);
        path.lineTo(72 * scaleX, 45 * scaleY);
        path.cubicTo(85 * scaleX, 45 * scaleY, 85 * scaleX, 32 * scaleY, 85 * scaleX, 22 * scaleY);


        Shader pathShader = new LinearGradient(0, 0, w, 0,
                new int[]{tealColor, blueColor, purpleColor},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
        pathPaint.setShader(pathShader);
        canvas.drawPath(path, pathPaint);
        pathPaint.setShader(null);

        float r1 = dp(6);
        float r2 = dp(4);
        circlePaint.setColor(tealColor);
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(15 * scaleX, 78 * scaleY, r1, circlePaint);
        circlePaint.setColor(0xFFFFFFFF);
        circlePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(15 * scaleX, 78 * scaleY, r1, circlePaint);

        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(50 * scaleX, 45 * scaleY, r2, circlePaint);
        circlePaint.setColor(blueColor);
        circlePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(50 * scaleX, 45 * scaleY, r2, circlePaint);

        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(redColor);
        canvas.drawCircle(85 * scaleX, 22 * scaleY, r1, circlePaint);
        circlePaint.setColor(0xFFFFFFFF);
        circlePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(85 * scaleX, 22 * scaleY, r1, circlePaint);
    }
}

