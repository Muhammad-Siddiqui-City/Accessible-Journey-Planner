package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * API response model for InstructionRef.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class InstructionRef implements Serializable {

    @SerializedName("summary")
    private String summary;

    public String getSummary() { return summary != null ? summary : ""; }

    public static InstructionRef withSummary(String text) {
        InstructionRef r = new InstructionRef();
        r.summary = text != null ? text : "";
        return r;
    }
}


