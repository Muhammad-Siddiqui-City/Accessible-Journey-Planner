package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Transport mode in Journey API. Add in Commit 2 (API DTOs).
 * PURPOSE: Mode name (e.g. "tube", "bus", "walking") for leg display and styling.
 * WHY: Used in StepsAdapter and RouteItem for step title and color.
 * ISSUES: None.
 */
public class ModeRef implements Serializable {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}
