package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;

/**
 * TfL arrival prediction for a stop. Add in Commit 2 (API DTOs).
 * PURPOSE: One predicted arrival: line, destination, platform, timeToStation (seconds), modeName.
 * WHY: StopPoint/{id}/Arrivals returns list of these; mapped to ui.arrivals.Arrival for display.
 * ISSUES: timeToStation is seconds; filter out <60s and >3600s in StopsViewModel.
 */
public class ArrivalPrediction {

    @SerializedName("lineName")
    private String lineName;
    @SerializedName("destinationName")
    private String destinationName;
    @SerializedName("platformName")
    private String platformName;
    @SerializedName("timeToStation")
    private int timeToStation;
    @SerializedName("modeName")
    private String modeName;

    public String getLineName() { return lineName != null ? lineName : ""; }
    public String getDestinationName() { return destinationName != null ? destinationName : ""; }
    public String getPlatformName() { return platformName != null ? platformName : ""; }
    public int getTimeToStation() { return timeToStation; }
    public String getModeName() { return modeName != null ? modeName : ""; }
}
