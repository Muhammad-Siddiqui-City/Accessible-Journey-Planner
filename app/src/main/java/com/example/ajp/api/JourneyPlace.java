package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Stop/place in Journey API. Add in Commit 2 (API DTOs).
 * PURPOSE: Departure/arrival point with name and coordinates; Serializable for intents.
 * WHY: Leg has departurePoint/arrivalPoint; used for map coords and display names.
 * ISSUES: None.
 */
public class JourneyPlace implements Serializable {

    @SerializedName("commonName")
    private String commonName;
    @SerializedName("lat")
    private double lat;
    @SerializedName("lon")
    private double lon;

    public String getCommonName() { return commonName != null ? commonName : ""; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
}
