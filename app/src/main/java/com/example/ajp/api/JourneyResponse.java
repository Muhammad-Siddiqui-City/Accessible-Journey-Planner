package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;







/**
 * DTO used to parse API payloads for JourneyResponse.
 */
public class JourneyResponse {

    @SerializedName("journeys")
    private List<Journey> journeys;

    public List<Journey> getJourneys() {
        return journeys != null ? journeys : java.util.Collections.emptyList();
    }
}

