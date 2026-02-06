package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * TfL Journey API response. Add in Commit 2 (API DTOs).
 * PURPOSE: Root response containing list of Journey options.
 * WHY: TfL returns { "journeys": [ ... ] }; Retrofit deserialises to this.
 * ISSUES: None.
 */
public class JourneyResponse {

    @SerializedName("journeys")
    private List<Journey> journeys;

    public List<Journey> getJourneys() {
        return journeys != null ? journeys : java.util.Collections.emptyList();
    }
}
