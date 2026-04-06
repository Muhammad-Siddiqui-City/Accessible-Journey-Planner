package com.example.ajp.ui.journey;

import com.example.ajp.api.Leg;
import java.io.Serializable;
import java.util.List;

/** Serializable route card: TfL legs, crowding, lift flag, optional POI warning. */
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

    /** Brand phrase for POI warning (resolved to UI language in RouteDetailsActivity, not here). */
    private final String poiVerifyBrandFrom;
    private final String poiVerifyBrandTo;
    /** Location part when origin brand+place was split (e.g. Canary Wharf); used in origin warning copy. */
    private final String poiVerifyLocationFrom;
    /** Location part when brand+place was split (e.g. Clapham Junction); used in destination warning copy. */
    private final String poiVerifyLocationTo;

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs) {
        this(durationMinutes, departureTime, arrivalTime, crowdingLevel, transfersText, lineBadges,
                routeSummary, routeId, fromStation, toStation, legs, false, null, null, null, null, null);
    }

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs, boolean hasLiftDisruption,
                     String liftDisruptionDescription) {
        this(durationMinutes, departureTime, arrivalTime, crowdingLevel, transfersText, lineBadges,
                routeSummary, routeId, fromStation, toStation, legs, hasLiftDisruption,
                liftDisruptionDescription, null, null, null, null);
    }

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs, boolean hasLiftDisruption,
                     String liftDisruptionDescription, String poiVerifyBrandFrom, String poiVerifyBrandTo) {
        this(durationMinutes, departureTime, arrivalTime, crowdingLevel, transfersText, lineBadges,
                routeSummary, routeId, fromStation, toStation, legs, hasLiftDisruption,
                liftDisruptionDescription, poiVerifyBrandFrom, null, poiVerifyBrandTo, null);
    }

    public RouteItem(String durationMinutes, String departureTime, String arrivalTime, int crowdingLevel,
                     String transfersText, String[] lineBadges, String routeSummary, String routeId,
                     String fromStation, String toStation, List<Leg> legs, boolean hasLiftDisruption,
                     String liftDisruptionDescription, String poiVerifyBrandFrom, String poiVerifyLocationFrom,
                     String poiVerifyBrandTo, String poiVerifyLocationTo) {
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
        this.poiVerifyBrandFrom = poiVerifyBrandFrom;
        this.poiVerifyBrandTo = poiVerifyBrandTo;
        this.poiVerifyLocationFrom = poiVerifyLocationFrom;
        this.poiVerifyLocationTo = poiVerifyLocationTo;
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

    public String getPoiVerifyBrandFrom() { return poiVerifyBrandFrom; }

    public String getPoiVerifyBrandTo() { return poiVerifyBrandTo; }

    public String getPoiVerifyLocationFrom() { return poiVerifyLocationFrom; }

    public String getPoiVerifyLocationTo() { return poiVerifyLocationTo; }

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

