package com.example.ajp.ui.arrivals;

/**
 * UI model for one arrival. Add in Commit 10.
 * PURPOSE: lineName, destinationName, platformName, timeToStationSeconds, modeName; from TfL or NationalRailApi.
 * WHY: ArrivalsAdapter displays these; timeToStationSeconds shown with TimeFormatUtil when >= 60 (minutes).
 * ISSUES: None.
 */
public class Arrival {

    private final String lineName;
    private final String destinationName;
    private final String platformName;
    private final int timeToStationSeconds;
    private final String modeName;

    public Arrival(String lineName, String destinationName, String platformName, int timeToStationSeconds, String modeName) {
        this.lineName = lineName != null ? lineName : "";
        this.destinationName = destinationName != null ? destinationName : "";
        this.platformName = platformName != null ? platformName : "";
        this.timeToStationSeconds = timeToStationSeconds >= 0 ? timeToStationSeconds : 0;
        this.modeName = modeName != null ? modeName : "";
    }

    public String getLineName() { return lineName; }
    public String getDestinationName() { return destinationName; }
    public String getPlatformName() { return platformName; }
    public int getTimeToStationSeconds() { return timeToStationSeconds; }
    public String getModeName() { return modeName; }
}
