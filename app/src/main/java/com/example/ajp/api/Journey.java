package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Gson DTO: Journey (TfL JSON field names). */
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

