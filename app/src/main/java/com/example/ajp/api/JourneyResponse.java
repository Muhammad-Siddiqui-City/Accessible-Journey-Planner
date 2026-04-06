package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Gson DTO: JourneyResponse (TfL JSON field names). */
public class JourneyResponse {

    @SerializedName("journeys")
    private List<Journey> journeys;

    public List<Journey> getJourneys() {
        return journeys != null ? journeys : java.util.Collections.emptyList();
    }
}

