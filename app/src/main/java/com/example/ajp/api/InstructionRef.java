package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;







/**
 * DTO used to parse API payloads for InstructionRef.
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

