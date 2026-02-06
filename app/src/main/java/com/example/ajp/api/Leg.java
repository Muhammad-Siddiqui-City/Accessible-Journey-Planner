package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * Single leg of a journey (walk, bus, tube, etc). Add in Commit 2 (API DTOs).
 * PURPOSE: One segment of a journey; Serializable for passing in intents.
 * WHY: TfL returns legs array; duration here is in SECONDS (unlike Journey.duration which is minutes).
 * ISSUES: When displaying leg duration, divide by 60 for minutes then use TimeFormatUtil.
 */
public class Leg implements Serializable {

    @SerializedName("departurePoint")
    private JourneyPlace departurePoint;
    @SerializedName("arrivalPoint")
    private JourneyPlace arrivalPoint;
    @SerializedName("mode")
    private ModeRef mode;
    @SerializedName("routeOptions")
    private List<RouteOptionRef> routeOptions;
    @SerializedName("instruction")
    private InstructionRef instruction;
    @SerializedName("crowding")
    private Crowding crowding;
    @SerializedName("duration")
    private Integer duration;

    public JourneyPlace getDeparturePoint() { return departurePoint; }
    public JourneyPlace getArrivalPoint() { return arrivalPoint; }
    public ModeRef getMode() { return mode; }
    public List<RouteOptionRef> getRouteOptions() { return routeOptions != null ? routeOptions : java.util.Collections.emptyList(); }
    public InstructionRef getInstruction() { return instruction; }
    public Crowding getCrowding() { return crowding; }
    public void setCrowding(Crowding crowding) { this.crowding = crowding; }
    public int getDuration() { return duration != null ? duration : 0; }
}
