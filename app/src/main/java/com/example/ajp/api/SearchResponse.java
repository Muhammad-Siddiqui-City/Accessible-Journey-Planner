package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * TfL StopPoint Search response (alternate shape). Add in Commit 2 (API DTOs).
 * PURPOSE: Some search endpoints return matches or stopPoints; getStopPoints() unifies.
 * WHY: Handles both response shapes for compatibility.
 * ISSUES: Prefer TflSearchResponse + MatchedStop for Search endpoint (Commit 8).
 */
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
