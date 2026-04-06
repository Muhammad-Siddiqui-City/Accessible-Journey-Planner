package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Gson DTO: TflSearchResponse (TfL JSON field names). */
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

