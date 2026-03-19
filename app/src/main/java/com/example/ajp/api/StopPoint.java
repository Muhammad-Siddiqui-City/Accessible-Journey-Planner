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
    @SerializedName("additionalProperties")
    private List<AdditionalProperty> additionalProperties;

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
    public List<AdditionalProperty> getAdditionalProperties() { return additionalProperties != null ? additionalProperties : java.util.Collections.emptyList(); }
    
    /**
     * Gets the number of lifts at this station from additionalProperties.
     * Returns -1 if lift information is not available (should not mark as problematic).
     * Returns 0 if explicitly set to 0 (no lifts - problematic).
     * Returns positive number if lifts are available.
     * Uses live data from TfL API.
     */
    public int getLiftCount() {
        if (additionalProperties == null) {
            android.util.Log.d("StopPoint", "getLiftCount: additionalProperties is null for station " + getCommonName() + " - returning -1 (no info)");
            return -1; // No information available - don't mark as problematic
        }
        android.util.Log.d("StopPoint", "getLiftCount: Checking " + additionalProperties.size() + " additional properties for station " + getCommonName());
        for (AdditionalProperty prop : additionalProperties) {
            if (prop != null) {
                android.util.Log.d("StopPoint", "getLiftCount: Property - category=" + prop.getCategory() + ", key=" + prop.getKey() + ", value=" + prop.getValue());
                if ("Facility".equals(prop.getCategory()) && "Lifts".equals(prop.getKey())) {
                    try {
                        String value = prop.getValue();
                        if (value != null && !value.trim().isEmpty()) {
                            int count = Integer.parseInt(value.trim());
                            android.util.Log.d("StopPoint", "getLiftCount: Found lift count=" + count + " for station " + getCommonName());
                            return count; // Explicit value (0 = no lifts, >0 = has lifts)
                        }
                    } catch (NumberFormatException e) {
                        android.util.Log.w("StopPoint", "getLiftCount: Failed to parse lift value: " + prop.getValue() + " - returning -1 (no info)");
                        return -1; // Can't parse - assume no info
                    }
                }
            }
        }
        android.util.Log.d("StopPoint", "getLiftCount: No lift property found for station " + getCommonName() + " - returning -1 (no info)");
        return -1; // Property doesn't exist - no information available, don't mark as problematic
    }
    
    /**
     * Checks if this station explicitly has no lifts (lift count is 0) or AccessViaLift=No.
     * Returns false if lift information is not available.
     */
    public boolean hasNoLifts() {
        // Check for explicit lift count of 0
        int count = getLiftCount();
        if (count == 0) return true;
        
        // Also check for AccessViaLift=No property (indicates no lift access)
        if (additionalProperties != null) {
            for (AdditionalProperty prop : additionalProperties) {
                if (prop != null && "Accessibility".equals(prop.getCategory()) && "AccessViaLift".equals(prop.getKey())) {
                    String value = prop.getValue();
                    if (value != null && ("No".equalsIgnoreCase(value.trim()) || "false".equalsIgnoreCase(value.trim()))) {
                        return true;
                    }
                }
            }
        }
        
        return false; // No info or has lifts
    }
    
    public boolean hasNationalRailMode() {
        if (modes == null) return false;
        for (String m : modes) {
            if (m != null && m.equalsIgnoreCase("national-rail")) return true;
        }
        return false;
    }

    /**
     * Additional property from TfL API (e.g., Facility information like lift count).
     * Used to parse additionalProperties array from StopPoint responses.
     */
    public static class AdditionalProperty {
        @SerializedName("category")
        private String category;
        @SerializedName("key")
        private String key;
        @SerializedName("value")
        private String value;

        public String getCategory() { return category != null ? category : ""; }
        public String getKey() { return key != null ? key : ""; }
        public String getValue() { return value != null ? value : ""; }
    }
}
