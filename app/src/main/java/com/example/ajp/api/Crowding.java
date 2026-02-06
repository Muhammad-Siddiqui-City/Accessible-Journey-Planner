package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * TfL Journey API crowding. Add in Commit 2 (API DTOs).
 * PURPOSE: crowdingLevel 1–5 (Very Quiet to Very Busy); used for route card and analytics.
 * WHY: Present on Tube/Rail legs when sensor data available; often absent for bus.
 * ISSUES: RouteItem/RouteAdapter map level to progress bar and color.
 */
public class Crowding implements Serializable {

    @SerializedName("crowdingLevel")
    private Integer crowdingLevel;

    public Integer getCrowdingLevel() {
        return crowdingLevel;
    }

    public void setCrowdingLevel(Integer crowdingLevel) {
        this.crowdingLevel = crowdingLevel;
    }
}
