package com.example.ajp.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import androidx.appcompat.widget.AppCompatTextView;





/**
 * Custom view for rendering LineBadge content.
 */
public class LineBadgeView extends AppCompatTextView {

    private static final int[][] LINE_COLORS = {
            { 0xFF0098D4, 0xFFFFFFFF },
            { 0xFFE32017, 0xFFFFFFFF },
            { 0xFF000000, 0xFFFFFFFF },
            { 0xFF003688, 0xFFFFFFFF },
            { 0xFFA0A5A9, 0xFF000000 },
            { 0xFFB36305, 0xFFFFFFFF },
            { 0xFFFFD300, 0xFF000000 },
            { 0xFF00782A, 0xFFFFFFFF },
            { 0xFF6950A1, 0xFFFFFFFF },
            { 0xFFEE7C0E, 0xFF000000 },
            { 0xFF00A4A7, 0xFFFFFFFF },
            { 0xFFE32017, 0xFFFFFFFF },
    };

    private static final String[] LINE_IDS = {
            "victoria", "central", "northern", "piccadilly", "jubilee",
            "bakerloo", "circle", "district", "elizabeth", "overground", "dlr", "bus"
    };

    private static final String[] LINE_ABBR = {
            "VIC", "CEN", "NOR", "PIC", "JUB", "BAK", "CIR", "DIS", "ELZ", "OVG", "DLR", "BUS"
    };

    public LineBadgeView(Context context) {
        super(context);
        init(null);
    }

    public LineBadgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LineBadgeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setGravity(Gravity.CENTER);
        setTypeface(Typeface.DEFAULT_BOLD);
        setTextSize(10);
        int padH = (int) (8 * getResources().getDisplayMetrics().density);
        int padV = (int) (4 * getResources().getDisplayMetrics().density);
        setPadding(padH, padV, padH, padV);
    }


    public void setLine(String lineId) {
        if (lineId == null) lineId = "";
        lineId = lineId.toLowerCase();
        int bg = 0xFF666666;
        int fg = 0xFFFFFFFF;
        String abbr = lineId.length() >= 3 ? lineId.substring(0, 3).toUpperCase() : "???";
        for (int i = 0; i < LINE_IDS.length; i++) {
            if (LINE_IDS[i].equals(lineId)) {
                bg = LINE_COLORS[i][0];
                fg = LINE_COLORS[i][1];
                abbr = LINE_ABBR[i];
                break;
            }
        }
        setBackgroundColor(bg);
        setTextColor(fg);
        setText(abbr);
    }
}

