package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;

/**
 * Single status entry for a line. Add in Commit 2 (API DTOs).
 * PURPOSE: statusSeverity and statusSeverityDescription for sorting/display.
 * WHY: StopsViewModel uses min severity across details to sort lines.
 * ISSUES: None.
 */
public class StatusDetail {

    @SerializedName("statusSeverity")
    private int statusSeverity;
    @SerializedName("statusSeverityDescription")
    private String statusSeverityDescription;
    @SerializedName("reason")
    private String reason;

    public int getStatusSeverity() { return statusSeverity; }
    public String getStatusSeverityDescription() { return statusSeverityDescription != null ? statusSeverityDescription : ""; }
    public String getReason() { return reason != null ? reason : ""; }
}
