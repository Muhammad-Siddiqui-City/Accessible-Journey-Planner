package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response model for TflSearchResponse.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
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


