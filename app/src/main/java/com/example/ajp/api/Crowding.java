package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/** Gson DTO: Crowding (TfL JSON field names). */
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

