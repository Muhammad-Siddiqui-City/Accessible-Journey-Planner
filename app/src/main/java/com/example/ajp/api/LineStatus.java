package com.example.ajp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Line status from TfL. Add in Commit 2 (API DTOs).
 * PURPOSE: One line's status (id, name, lineStatuses) for disruption display.
 * WHY: getLineStatus returns List<LineStatus>; used for "Good Service" or disruption text.
 * ISSUES: None.
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
