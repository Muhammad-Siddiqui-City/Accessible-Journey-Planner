package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Leg instruction from TfL. Add in Commit 2 (API DTOs).
 * PURPOSE: Human-readable step summary (e.g. "District line to Wimbledon").
 * WHY: instruction.summary used in route details and share text.
 * ISSUES: None.
 */
public class InstructionRef implements Serializable {

    @SerializedName("summary")
    private String summary;

    public String getSummary() { return summary != null ? summary : ""; }
}
