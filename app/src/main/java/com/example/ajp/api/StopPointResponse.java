package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Gson DTO: StopPointResponse (TfL JSON field names). */
public class StopPointResponse {

    @SerializedName("stopPoints")
    private List<StopPoint> stopPoints;

    public List<StopPoint> getStopPoints() {
        return stopPoints;
    }
}

