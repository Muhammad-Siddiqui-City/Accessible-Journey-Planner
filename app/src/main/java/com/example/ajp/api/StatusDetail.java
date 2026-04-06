package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;

/**
 * API response model for StatusDetail.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
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


