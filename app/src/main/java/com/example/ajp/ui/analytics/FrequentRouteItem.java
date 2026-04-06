package com.example.ajp.ui.analytics;

/** Summary line for frequent-routes list on Analytics. */
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

