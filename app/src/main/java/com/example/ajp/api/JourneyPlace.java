package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * API response model for JourneyPlace.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class JourneyPlace implements Serializable {

    @SerializedName("commonName")
    private String commonName;
    @SerializedName("lat")
    private double lat;
    @SerializedName("lon")
    private double lon;

    @SerializedName("naptanId")
    private String naptanId;

    public String getCommonName() { return commonName != null ? commonName : ""; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getNaptanId() { return naptanId != null ? naptanId : ""; }

    /** Synthetic point for walking legs (not from Gson). */
    public static JourneyPlace at(String commonName, double lat, double lon) {
        JourneyPlace p = new JourneyPlace();
        p.commonName = commonName != null ? commonName : "";
        p.lat = lat;
        p.lon = lon;
        p.naptanId = "";
        return p;
    }
}


