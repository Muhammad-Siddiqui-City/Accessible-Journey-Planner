package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Single stop point from TfL API. Add in Commit 2 (API DTOs).
 * PURPOSE: Nearby stop with id, name, distance, lines, stopType; children for NR platforms.
 * WHY: getNearbyBuses/getNearbyTrains return StopPointResponse of these; hasNationalRailMode
 *      used to decide National Rail fallback (Commit 9).
 * ISSUES: getNaptanId() falls back to id if naptanId null; children may be platform-level.
 */
public class StopPoint {

    private static final String STOP_TYPE_METRO = "NaptanMetroStation";
    private static final String STOP_TYPE_RAIL = "NaptanRailStation";

    @SerializedName("naptanId")
    private String naptanId;
    @SerializedName("id")
    private String id;
    @SerializedName("commonName")
    private String commonName;
    @SerializedName("distance")
    private double distance;
    @SerializedName("lines")
    private List<LineIdentifier> lines;
    @SerializedName("stopType")
    private String stopType;
    @SerializedName("stopLetter")
    private String stopLetter;
    @SerializedName("children")
    private List<StopPoint> children;
    @SerializedName("modes")
    private List<String> modes;

    public String getNaptanId() { return naptanId != null ? naptanId : id; }
    public String getCommonName() { return commonName != null ? commonName : ""; }
    public double getDistance() { return distance; }
    public List<LineIdentifier> getLines() { return lines; }
    public String getStopType() { return stopType != null ? stopType : ""; }
    public String getStopLetter() { return stopLetter != null ? stopLetter : ""; }
    public void setStopLetter(String stopLetter) { this.stopLetter = stopLetter; }
    public boolean isTubeStation() { return STOP_TYPE_METRO.equals(stopType); }
    public boolean isRailStation() { return STOP_TYPE_RAIL.equals(stopType); }
    public List<StopPoint> getChildren() { return children != null ? children : java.util.Collections.emptyList(); }
    public boolean hasNationalRailMode() {
        if (modes == null) return false;
        for (String m : modes) {
            if (m != null && m.equalsIgnoreCase("national-rail")) return true;
        }
        return false;
    }
}
