package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;

/**
 * Line reference in TfL API. Add in Commit 2 (API DTOs).
 * PURPOSE: Line id/name for badges (e.g. "District", "Piccadilly").
 * WHY: StopPoint has List<LineIdentifier>; used in nearby list and route badges.
 * ISSUES: None.
 */
public class LineIdentifier {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}
