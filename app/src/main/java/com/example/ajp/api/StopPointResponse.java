package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;







/**
 * DTO used to parse API payloads for StopPointResponse.
 */
public class StopPointResponse {

    @SerializedName("stopPoints")
    private List<StopPoint> stopPoints;

    public List<StopPoint> getStopPoints() {
        return stopPoints;
    }
}

