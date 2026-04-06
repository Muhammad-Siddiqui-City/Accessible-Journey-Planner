package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response model for StopPointResponse.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class StopPointResponse {

    @SerializedName("stopPoints")
    private List<StopPoint> stopPoints;

    public List<StopPoint> getStopPoints() {
        return stopPoints;
    }
}


