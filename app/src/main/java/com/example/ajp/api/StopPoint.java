package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * API response model for StopPoint.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class StopPoint {

    private static final String STOP_TYPE_METRO = "NaptanMetroStation";
    private static final String STOP_TYPE_RAIL = "NaptanRailStation";

    @SerializedName("naptanId")
    private String naptanId;
    @SerializedName("lat")
    private double lat;
    @SerializedName("lon")
    private double lon;
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
    /** WGS84 latitude when TfL returns coordinates (nearby search, detail). */
    public double getLat() { return lat; }
    /** WGS84 longitude when TfL returns coordinates (nearby search, detail). */
    public double getLon() { return lon; }
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

    public int getLiftCount() {
        if (additionalProperties == null) {
            android.util.Log.d("StopPoint", "getLiftCount: additionalProperties is null for station " + getCommonName() + " - returning -1 (no info)");
            return -1;
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
                            return count;
                        }
                    } catch (NumberFormatException e) {
                        android.util.Log.w("StopPoint", "getLiftCount: Failed to parse lift value: " + prop.getValue() + " - returning -1 (no info)");
                        return -1;
                    }
                }
            }
        }
        android.util.Log.d("StopPoint", "getLiftCount: No lift property found for station " + getCommonName() + " - returning -1 (no info)");
        return -1;
    }

    public boolean hasNoLifts() {

        int count = getLiftCount();
        if (count == 0) return true;

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

        return false;
    }

    public boolean hasNationalRailMode() {
        if (modes == null) return false;
        for (String m : modes) {
            if (m != null && m.equalsIgnoreCase("national-rail")) return true;
        }
        return false;
    }

    private static final Pattern THREE_LETTER = Pattern.compile("^[A-Za-z]{3}$");

    public String getCrsFromAdditionalProperties() {
        if (additionalProperties == null) return null;
        for (AdditionalProperty p : additionalProperties) {
            if (p == null) continue;
            String k = p.getKey();
            String v = p.getValue();
            if (v == null) continue;
            String t = v.trim();
            if (t.length() != 3 || !THREE_LETTER.matcher(t).matches()) continue;
            if (k == null) continue;
            String kl = k.toLowerCase(Locale.UK);
            if (kl.contains("crs") || kl.contains("stationcode") || kl.contains("3letter") || kl.contains("tlc")) {
                return t.toUpperCase(Locale.UK);
            }
        }
        return null;
    }

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


