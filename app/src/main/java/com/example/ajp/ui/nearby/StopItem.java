package com.example.ajp.ui.nearby;

/** Immutable stop/search list row: id, name, distance, line codes, step-free, station flag. */
public class StopItem {

    private final String stopId;
    private final String name;

    private final double distance;
    private final String[] lineCodes;
    private final boolean stepFree;

    private final String stopLetter;

    private final boolean isStation;

    public StopItem(String stopId, String name, double distanceMeters, String[] lineCodes, boolean stepFree, String stopLetter) {
        this(stopId, name, distanceMeters, lineCodes, stepFree, stopLetter, false);
    }

    public StopItem(String stopId, String name, double distanceMeters, String[] lineCodes, boolean stepFree, String stopLetter, boolean isStation) {
        this.stopId = stopId != null ? stopId : "";
        this.name = name;
        this.distance = distanceMeters >= 0 ? distanceMeters : 0;
        this.lineCodes = lineCodes != null ? lineCodes : new String[0];
        this.stepFree = stepFree;
        this.stopLetter = stopLetter != null ? stopLetter : "";
        this.isStation = isStation;
    }

    public String getStopId() { return stopId; }
    public String getName() { return name; }

    public double getDistance() { return distance; }
    public String[] getLineCodes() { return lineCodes; }
    public boolean isStepFree() { return stepFree; }
    public String getStopLetter() { return stopLetter; }

    public boolean isStation() { return isStation; }
}

