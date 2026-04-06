package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Gson DTO: LineStatus (TfL JSON field names). */
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

