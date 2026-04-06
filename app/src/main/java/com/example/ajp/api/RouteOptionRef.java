package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * API response model for RouteOptionRef.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class RouteOptionRef implements Serializable {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}


