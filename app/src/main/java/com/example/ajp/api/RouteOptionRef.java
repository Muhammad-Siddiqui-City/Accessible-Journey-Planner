package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/** Gson DTO: RouteOptionRef (TfL JSON field names). */
public class RouteOptionRef implements Serializable {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}

