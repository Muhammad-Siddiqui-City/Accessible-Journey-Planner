package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response model for Journey.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class Journey {

    @SerializedName("startDateTime")
    private String startDateTime;
    @SerializedName("duration")
    private int duration;
    @SerializedName("arrivalDateTime")
    private String arrivalDateTime;
    @SerializedName("legs")
    private List<Leg> legs;

    public String getStartDateTime() { return startDateTime != null ? startDateTime : ""; }
    public int getDuration() { return duration; }
    public String getArrivalDateTime() { return arrivalDateTime != null ? arrivalDateTime : ""; }
    public List<Leg> getLegs() { return legs != null ? legs : java.util.Collections.emptyList(); }
}


