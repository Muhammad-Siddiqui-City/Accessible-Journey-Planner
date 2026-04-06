package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;

/**
 * API response model for LineIdentifier.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class LineIdentifier {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}


