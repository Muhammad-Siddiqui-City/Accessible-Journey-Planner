package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * TfL StopPoint Search response. Add in Commit 2 (API DTOs).
 * PURPOSE: query, total, matches (MatchedStop) from StopPoint/Search/{query}.
 * WHY: Used in StationSearchFragment; merge with PlaceSearch results in Commit 14.
 * ISSUES: None.
 */
public class TflSearchResponse {

    @SerializedName("query")
    private String query;
    @SerializedName("total")
    private int total;
    @SerializedName("matches")
    private List<MatchedStop> matches;

    public List<MatchedStop> getMatches() {
        return matches != null ? matches : java.util.Collections.emptyList();
    }
}
