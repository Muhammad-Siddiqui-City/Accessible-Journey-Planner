package com.example.ajp.ui.arrivals;

/**
 * UI-side model/helper used by Arrival.
 * Encapsulates presentation-oriented behavior needed by screens in this feature package.
 * Keeping this separate helps avoid leaking API/database concerns into view code.
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


