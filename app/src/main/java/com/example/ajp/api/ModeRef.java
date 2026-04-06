package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * API response model for ModeRef.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class ModeRef implements Serializable {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }

    public static ModeRef named(String modeName) {
        ModeRef m = new ModeRef();
        m.name = modeName != null ? modeName : "";
        return m;
    }
}


