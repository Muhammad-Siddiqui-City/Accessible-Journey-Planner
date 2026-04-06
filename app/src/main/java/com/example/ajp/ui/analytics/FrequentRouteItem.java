package com.example.ajp.ui.analytics;

/**
 * UI-side model/helper used by FrequentRouteItem.
 * Encapsulates presentation-oriented behavior needed by screens in this feature package.
 * Keeping this separate helps avoid leaking API/database concerns into view code.
 */

public class FrequentRouteItem {
    private final String routeLabel;
    private final String detail;
    private final int count;

    public FrequentRouteItem(String routeLabel, String detail, int count) {
        this.routeLabel = routeLabel != null ? routeLabel : "";
        this.detail = detail != null ? detail : "";
        this.count = count;
    }

    public String getRouteLabel() { return routeLabel; }
    public String getDetail() { return detail; }
    public int getCount() { return count; }
}


