package com.example.ajp.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.ajp.ui.journey.RouteItem;

/**
 * Room entity for saved routes. Add in Commit 4.
 * PURPOSE: Store RouteItem as JSON (via Converters), timestamp, summary for "saved routes" list.
 * WHY: Single table; RouteItem is complex so use TypeConverter not embedded fields.
 * ISSUES: RouteItem must be Gson-serialisable; Converters registered in AppDatabase.
 */
@Entity(tableName = "saved_routes")
@TypeConverters(Converters.class)
public class SavedRouteEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "route_json")
    public RouteItem routeItem;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "summary")
    public String summary;

    public SavedRouteEntity() { }

    @Ignore
    public SavedRouteEntity(RouteItem routeItem, long timestamp, String summary) {
        this.routeItem = routeItem;
        this.timestamp = timestamp;
        this.summary = summary;
    }
}
