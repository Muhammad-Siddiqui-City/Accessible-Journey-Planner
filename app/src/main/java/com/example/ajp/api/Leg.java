package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;







/**
 * DTO used to parse API payloads for Leg.
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

    /**
     * Walking leg not returned by TfL (e.g. between a geocoded place and the nearest stop).
     */
    public static Leg createWalkingLeg(JourneyPlace departure, JourneyPlace arrival, int durationSeconds,
                                       InstructionRef instruction) {
        Leg leg = new Leg();
        leg.departurePoint = departure;
        leg.arrivalPoint = arrival;
        leg.mode = ModeRef.named("walking");
        leg.duration = durationSeconds;
        leg.instruction = instruction; // null for app-added legs (no subtitle / no time line in UI)
        return leg;
    }
}

