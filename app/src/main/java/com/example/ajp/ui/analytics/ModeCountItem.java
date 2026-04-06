package com.example.ajp.ui.analytics;

/** Label + count for analytics mode donut. */
public class ModeCountItem {
    private final String mode;
    private final int count;
    private final int color;

    public ModeCountItem(String mode, int count, int color) {
        this.mode = mode != null ? mode : "";
        this.count = count;
        this.color = color;
    }

    public String getMode() { return mode; }
    public int getCount() { return count; }
    public int getColor() { return color; }
}

