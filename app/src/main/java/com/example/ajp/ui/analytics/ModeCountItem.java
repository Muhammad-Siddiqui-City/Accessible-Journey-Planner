package com.example.ajp.ui.analytics;

/**
 * Transport mode + count + color for analytics pie legend. Add in Commit 15.
 * PURPOSE: mode name, count, color (TfL-style); used by pie chart and legend rows.
 * WHY: AnalyticsViewModel.getTflColorForMode maps mode to color; createLegendRow builds legend UI.
 * ISSUES: None.
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
