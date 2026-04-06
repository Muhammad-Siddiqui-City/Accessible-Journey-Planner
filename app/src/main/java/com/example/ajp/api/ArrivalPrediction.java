package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;

/**
 * API response model for ArrivalPrediction.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
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


