package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;







/**
 * DTO used to parse API payloads for JourneyPlace.
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
}

