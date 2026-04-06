package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/** Gson DTO: ModeRef (TfL JSON field names). */
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

