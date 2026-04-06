package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;

/** Gson DTO: LineIdentifier (TfL JSON field names). */
public class LineIdentifier {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}

