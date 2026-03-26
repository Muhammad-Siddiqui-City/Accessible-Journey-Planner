package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;







/**
 * DTO used to parse API payloads for LineIdentifier.
 */
public class LineIdentifier {

    @SerializedName("name")
    private String name;

    public String getName() { return name != null ? name : ""; }
}

