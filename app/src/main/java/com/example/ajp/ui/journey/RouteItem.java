package com.example.ajp.ui.journey;

// AI Generated
// Built with Claude
// Lovable.dev reference

import com.example.ajp.api.Leg;
import java.io.Serializable;
import java.util.List;

/**
 * UI-side model/helper used by RouteItem.
 * Encapsulates presentation-oriented behavior needed by screens in this feature package.
 * Keeping this separate helps avoid leaking API/database concerns into view code.
 */

public class RouteItem implements Serializable {

    public static final int CROWDING_LOW = 0;
    public static final int CROWDING_MEDIUM = 1;
    public static final int CROWDING_HIGH = 2;

    private final String durationMinutes;
    private final String departureTime;
    private final String arrivalTime;
    private final int crowdingLevel;
    private final String transfersText;
    private final String[] lineBadges;
    private final String routeSummary;
    private final String routeId;
    private final String fromStation;
    private final String toStation;
    private final List<Leg> legs;
    private final boolean hasLiftDisruption;

    private final String liftDisruptionDescription;

    /** Full sentence when user typed brand + location (e.g. cannot verify shop exists). */
    private final String poiVerificationWarning;

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs) {
        this(durationMinutes, departureTime, arrivalTime, crowdingLevel, transfersText, lineBadges,
                routeSummary, routeId, fromStation, toStation, legs, false, null, null);
    }

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs, boolean hasLiftDisruption,
                     String liftDisruptionDescription) {
        this(durationMinutes, departureTime, arrivalTime, crowdingLevel, transfersText, lineBadges,
                routeSummary, routeId, fromStation, toStation, legs, hasLiftDisruption,
                liftDisruptionDescription, null);
    }

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs, boolean hasLiftDisruption,
                     String liftDisruptionDescription, String poiVerificationWarning) {
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
        this.poiVerificationWarning = poiVerificationWarning;
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

    public String getLiftDisruptionDescription() { return liftDisruptionDescription; }

    public String getPoiVerificationWarning() { return poiVerificationWarning; }

    public int getDurationMinutesInt() {
        if (durationMinutes == null || durationMinutes.isEmpty()) return 0;
        try {
            return Integer.parseInt(durationMinutes.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public int getTransfersCount() {
        if (transfersText == null || transfersText.isEmpty()) return 0;
        try {
            String num = transfersText.replaceAll("[^0-9]", "");
            return num.isEmpty() ? 0 : Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

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
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteItem)) return false;
        RouteItem that = (RouteItem) o;
        return routeId != null && routeId.equals(that.routeId);
    }

    @Override
    // Evaluates a condition used to branch behavior in the surrounding flow.
    public int hashCode() {
        return routeId != null ? routeId.hashCode() : 0;
    }
}


