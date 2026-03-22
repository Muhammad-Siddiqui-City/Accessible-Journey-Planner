package com.example.ajp.ui.nearby;

/**
 * Model for a nearby stop or place. Add in Commit 8; used for places in Commit 14 (id = "lat,lon", lineCodes = ["Place"]).
 * PURPOSE: stopId, name, distance, lineCodes, stepFree, stopLetter, isStation; used in nearby list and search.
 * WHY: PlaceSearch returns StopItem with id "lat,lon" so JourneyViewModel can use as from/to; StopsViewModel skips arrivals when id contains ",".
 * ISSUES: None.
 */
public class StopItem {

    private final String stopId;
    private final String name;
    /** Distance in meters (used for sort and to display in km). */
    private final double distance;
    private final String[] lineCodes;
    private final boolean stepFree;
    /** Stop letter from TfL (e.g. "V" for bus stand); empty for rail/tube. */
    private final String stopLetter;
    /** True if Tube/Rail/Overground/DLR/Elizabeth; false if bus. Used to prioritise stations over buses in list. */
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
    /** Distance in meters (for sorting and adapter displays in km). */
    public double getDistance() { return distance; }
    public String[] getLineCodes() { return lineCodes; }
    public boolean isStepFree() { return stepFree; }
    public String getStopLetter() { return stopLetter; }
    /** True if this is a station (Tube/Rail/Overground/DLR/Elizabeth); false if bus stop. */
    public boolean isStation() { return isStation; }
}
