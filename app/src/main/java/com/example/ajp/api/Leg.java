package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/** Gson DTO: Leg (TfL JSON field names). */
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

    /** App-only: first leg from geocoded pin to nearest stop (not in TfL JSON). */
    private boolean syntheticOriginConnector;
    /** When {@link #syntheticOriginConnector}, true if nearest stop is not tube/rail (e.g. bus stop on a street). */
    private boolean syntheticConnectorBusStop;

    public JourneyPlace getDeparturePoint() { return departurePoint; }
    public JourneyPlace getArrivalPoint() { return arrivalPoint; }
    public ModeRef getMode() { return mode; }
    public List<RouteOptionRef> getRouteOptions() { return routeOptions != null ? routeOptions : java.util.Collections.emptyList(); }
    public InstructionRef getInstruction() { return instruction; }
    public Crowding getCrowding() { return crowding; }
    public void setCrowding(Crowding crowding) { this.crowding = crowding; }
    public int getDuration() { return duration != null ? duration : 0; }

    public boolean isSyntheticOriginConnector() { return syntheticOriginConnector; }

    public boolean isSyntheticConnectorBusStop() { return syntheticConnectorBusStop; }

    /**
     * Walking leg not returned by TfL (e.g. between a geocoded place and the nearest stop).
     */
    public static Leg createWalkingLeg(JourneyPlace departure, JourneyPlace arrival, int durationSeconds,
                                       InstructionRef instruction) {
        return createWalkingLeg(departure, arrival, durationSeconds, instruction, false, false);
    }

    /**
     * @param syntheticOriginConnector pin→nearest-stop connector (show bus stop / train station wording).
     * @param syntheticConnectorBusStop  true when nearest stop is not tube/rail (street-level bus stop, etc.).
     */
    public static Leg createWalkingLeg(JourneyPlace departure, JourneyPlace arrival, int durationSeconds,
                                       InstructionRef instruction, boolean syntheticOriginConnector,
                                       boolean syntheticConnectorBusStop) {
        Leg leg = new Leg();
        leg.departurePoint = departure;
        leg.arrivalPoint = arrival;
        leg.mode = ModeRef.named("walking");
        leg.duration = durationSeconds;
        leg.instruction = instruction;
        leg.syntheticOriginConnector = syntheticOriginConnector;
        leg.syntheticConnectorBusStop = syntheticConnectorBusStop;
        return leg;
    }
}

