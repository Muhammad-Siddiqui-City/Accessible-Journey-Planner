package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response model for LineStatus.
 * Matches remote payload fields so parsing remains predictable and explicit.
 * This keeps network schema changes localized to model classes and mappers.
 */

public class LineStatus {

    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("lineStatuses")
    private List<StatusDetail> lineStatuses;

    public String getId() { return id != null ? id : ""; }
    public String getName() { return name != null ? name : ""; }
    public List<StatusDetail> getLineStatuses() { return lineStatuses; }
}


