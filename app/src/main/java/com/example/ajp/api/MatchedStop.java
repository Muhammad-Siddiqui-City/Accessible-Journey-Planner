package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Single match from TfL StopPoint Search. Add in Commit 2 (API DTOs).
 * PURPOSE: One search result: id, name, lat, lon, modes.
 * WHY: TflSearchResponse.getMatches() returns these; map to StopItem for search list.
 * ISSUES: None.
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
