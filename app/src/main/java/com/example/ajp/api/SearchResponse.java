package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Gson DTO: SearchResponse (TfL JSON field names). */
public class SearchResponse {

    @SerializedName("matches")
    private List<StopPoint> matches;

    @SerializedName("stopPoints")
    private List<StopPoint> stopPoints;

    public List<StopPoint> getStopPoints() {
        if (matches != null && !matches.isEmpty()) return matches;
        if (stopPoints != null) return stopPoints;
        return java.util.Collections.emptyList();
    }
}

