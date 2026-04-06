package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response model for MatchedStop.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class MatchedStop {

    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("lat")
    private double lat;
    @SerializedName("lon")
    private double lon;
    @SerializedName("modes")
    private List<String> modes;

    public String getId() { return id != null ? id : ""; }
    public String getName() { return name != null ? name : ""; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public List<String> getModes() { return modes != null ? modes : java.util.Collections.emptyList(); }
}


