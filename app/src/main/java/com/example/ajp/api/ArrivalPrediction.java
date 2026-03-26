package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;







/**
 * DTO used to parse API payloads for ArrivalPrediction.
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

