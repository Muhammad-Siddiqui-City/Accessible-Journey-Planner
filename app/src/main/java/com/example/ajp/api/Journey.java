package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Single journey from TfL Journey API. Add in Commit 2 (API DTOs).
 * PURPOSE: Deserialise journey plan response; duration in MINUTES (TfL), legs in order.
 * WHY: Gson @SerializedName matches TfL JSON; getDuration() used for route summary (use TimeFormatUtil for display).
 * ISSUES: Leg.getDuration() is in SECONDS; Journey.getDuration() in minutes – document to avoid bugs.
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
