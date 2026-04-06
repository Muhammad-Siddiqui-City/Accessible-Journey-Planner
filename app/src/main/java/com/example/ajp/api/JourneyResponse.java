package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response model for JourneyResponse.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class JourneyResponse {

    @SerializedName("journeys")
    private List<Journey> journeys;

    public List<Journey> getJourneys() {
        return journeys != null ? journeys : java.util.Collections.emptyList();
    }
}


