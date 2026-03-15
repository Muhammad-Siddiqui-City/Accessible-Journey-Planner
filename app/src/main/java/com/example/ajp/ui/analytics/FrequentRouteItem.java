package com.example.ajp.ui.analytics;

/**
 * One row for Frequent Routes list. Add in Commit 15.
 * PURPOSE: routeLabel (From → To), detail (mode + ~duration from most recent JourneyLog), count (trip count).
 * WHY: Built in AnalyticsViewModel from journey logs; detail uses TimeFormatUtil for duration display.
 * ISSUES: None.
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
