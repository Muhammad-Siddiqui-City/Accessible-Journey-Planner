package com.example.ajp.ui.analytics;

/**
 * UI-side model/helper used by ModeCountItem.
 * Encapsulates presentation-oriented behavior needed by screens in this feature package.
 * Keeping this separate helps avoid leaking API/database concerns into view code.
 */

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


