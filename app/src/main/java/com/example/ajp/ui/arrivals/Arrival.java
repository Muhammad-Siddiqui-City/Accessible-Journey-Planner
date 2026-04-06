package com.example.ajp.ui.arrivals;

/** One arrival row for adapters (TfL or National Rail normalised). */
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

