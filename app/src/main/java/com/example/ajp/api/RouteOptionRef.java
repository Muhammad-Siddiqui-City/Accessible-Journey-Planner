package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;







/**
 * DTO used to parse API payloads for RouteOptionRef.
 */
public class RouteOptionRef implements Serializable {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}

