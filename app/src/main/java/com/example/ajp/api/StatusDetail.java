package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;







/**
 * DTO used to parse API payloads for StatusDetail.
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

