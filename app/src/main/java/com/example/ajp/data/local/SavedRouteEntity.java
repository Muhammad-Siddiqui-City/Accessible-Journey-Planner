package com.example.ajp.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.ajp.ui.journey.RouteItem;

@Entity(tableName = "saved_routes")
@TypeConverters(Converters.class)
/**
 * Room entity model for SavedRouteEntity.
 * Represents one persisted record and maps fields directly to table columns.
 * Entity-level constraints and names are kept close to the data model for clarity.
 */

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

