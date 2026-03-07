package com.example.ajp.ui.journey;

import com.example.ajp.api.Leg;
import java.io.Serializable;
import java.util.List;

/**
 * Model for a suggested route card. Add in Commit 11.
 * PURPOSE: durationMinutes (string), departure/arrival, crowding level, line badges, legs; Serializable for intent.
 * WHY: Built from Journey + Legs; getDurationMinutesInt() for RouteOptimizer; getTotalWalkingMinutes from walking legs.
 * ISSUES: durationMinutes is string (e.g. "74"); display with TimeFormatUtil.formatMinutesToHourMin(getDurationMinutesInt()).
 */
public class RouteItem implements Serializable {

    public static final int CROWDING_LOW = 0;
    public static final int CROWDING_MEDIUM = 1;
    public static final int CROWDING_HIGH = 2;

    private final String durationMinutes;
    private final String departureTime;
    private final String arrivalTime;
    private final int crowdingLevel; // CROWDING_LOW, CROWDING_MEDIUM, CROWDING_HIGH
    private final String transfersText;
    private final String[] lineBadges; // e.g. {"VIC"} or {"PIC", "JUB"}
    private final String routeSummary;
    private final String routeId;
    private final String fromStation;
    private final String toStation;
    private final List<Leg> legs;
    private final boolean hasLiftDisruption;
    // AI Generated
    private final String liftDisruptionDescription;

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs) {
        this(durationMinutes, departureTime, arrivalTime, crowdingLevel, transfersText, lineBadges,
                routeSummary, routeId, fromStation, toStation, legs, false, null);
    }

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs, boolean hasLiftDisruption,
                     String liftDisruptionDescription) {
        this.durationMinutes = durationMinutes;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime != null ? arrivalTime : "";
        this.crowdingLevel = crowdingLevel;
        this.transfersText = transfersText;
        this.lineBadges = lineBadges != null ? lineBadges : new String[0];
        this.routeSummary = routeSummary;
        this.routeId = routeId;
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
        this.legs = legs != null ? legs : java.util.Collections.emptyList();
        this.hasLiftDisruption = hasLiftDisruption;
        this.liftDisruptionDescription = liftDisruptionDescription;
    }

    public String getDurationMinutes() { return durationMinutes; }
    public String getDepartureTime() { return departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public int getCrowdingLevel() { return crowdingLevel; }
    public String getTransfersText() { return transfersText; }
    public String[] getLineBadges() { return lineBadges; }
    public String getRouteSummary() { return routeSummary; }
    public String getRouteId() { return routeId; }
    public String getFromStation() { return fromStation; }
    public String getToStation() { return toStation; }
    public List<Leg> getLegs() { return legs; }
    public boolean hasLiftDisruption() { return hasLiftDisruption; }
    // AI Generated
    /** Actual TfL disruption text (e.g. "No step-free access to/from the eastbound platform"). Null if none. */
    public String getLiftDisruptionDescription() { return liftDisruptionDescription; }

    /** Parsed duration in minutes for scoring (e.g. 15 from "15"). */
    public int getDurationMinutesInt() {
        if (durationMinutes == null || durationMinutes.isEmpty()) return 0;
        try {
            return Integer.parseInt(durationMinutes.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /** Transfer count parsed from transfersText (e.g. "2 transfers" -> 2). */
    public int getTransfersCount() {
        if (transfersText == null || transfersText.isEmpty()) return 0;
        try {
            String num = transfersText.replaceAll("[^0-9]", "");
            return num.isEmpty() ? 0 : Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Total walking time in minutes from walking legs. */
    public int getTotalWalkingMinutes() {
        if (legs == null || legs.isEmpty()) return 0;
        int seconds = 0;
        for (Leg leg : legs) {
            if (leg.getMode() != null && "walking".equalsIgnoreCase(leg.getMode().getName())) {
                seconds += leg.getDuration();
            }
        }
        return seconds / 60;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteItem)) return false;
        RouteItem that = (RouteItem) o;
        return routeId != null && routeId.equals(that.routeId);
    }

    @Override
    public int hashCode() {
        return routeId != null ? routeId.hashCode() : 0;
    }
}
