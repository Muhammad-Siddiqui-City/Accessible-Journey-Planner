package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * API response model for Crowding.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
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


