package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Route option in Journey API leg. Add in Commit 2 (API DTOs).
 * PURPOSE: Line name for leg (e.g. "District Line"); used in route summary/badges.
 * WHY: Leg has list of routeOptions; first used for line badge when building RouteItem.
 * ISSUES: None.
 */
public class RouteOptionRef implements Serializable {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}
