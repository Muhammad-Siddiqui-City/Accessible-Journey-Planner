package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * TfL API response for nearby stops. Add in Commit 2 (API DTOs).
 * PURPOSE: Wrapper containing stopPoints list from getNearbyBuses/getNearbyTrains.
 * WHY: TfL returns { "stopPoints": [ ... ] }.
 * ISSUES: getStopPoints() can return null; callers should null-check.
 */
public class StopPointResponse {

    @SerializedName("stopPoints")
    private List<StopPoint> stopPoints;

    public List<StopPoint> getStopPoints() {
        return stopPoints;
    }
}
