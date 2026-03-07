package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * TfL StopPoint disruption DTO.
 * PURPOSE: Deserialise StopPoint/{id}/disruption response; description may contain lift/escalator outage text.
 * WHY: LiftDisruptionChecker parses description for keywords like "lift", "escalator" to detect step-free issues.
 * ISSUES: TfL uses atcoCode/stationAtcoCode; Journey legs use NaPTAN IDs - both refer to the same station.
 */
public class Disruption implements Serializable {

    @SerializedName("description")
    private String description;
    // AI Generated
    @SerializedName("additionalInfo")
    private String additionalInfo;
    @SerializedName("commonName")
    private String commonName;
    @SerializedName("type")
    private String type;
    @SerializedName("mode")
    private String mode;
    @SerializedName("appearance")
    private String appearance;

    public String getDescription() {
        return description != null ? description : "";
    }

    // AI Generated
    /** Returns best display text (description or additionalInfo). TfL may put step-free text in either. */
    public String getDisplayText() {
        if (description != null && !description.trim().isEmpty()) return description.trim();
        if (additionalInfo != null && !additionalInfo.trim().isEmpty()) return additionalInfo.trim();
        return "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getAdditionalInfo() {
        return additionalInfo != null ? additionalInfo : "";
    }

    public String getCommonName() {
        return commonName != null ? commonName : "";
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getType() {
        return type != null ? type : "";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode != null ? mode : "";
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getAppearance() {
        return appearance != null ? appearance : "";
    }

    public void setAppearance(String appearance) {
        this.appearance = appearance;
    }
}
