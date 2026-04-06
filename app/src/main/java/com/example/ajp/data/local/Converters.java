package com.example.ajp.data.local;

import androidx.room.TypeConverter;
import com.example.ajp.ui.journey.RouteItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Type converters used by Room for non-primitive fields.
 * Serializes complex route objects so they can be stored in SQLite columns safely.
 * This preserves model fidelity without exploding the table schema.
 */

public class Converters {

    private static final Gson GSON = new Gson();

    @TypeConverter
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static String fromRouteItem(RouteItem route) {
        if (route == null) return null;
        return GSON.toJson(route);
    }

    @TypeConverter
    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static RouteItem toRouteItem(String json) {
        if (json == null || json.isEmpty()) return null;
        return GSON.fromJson(json, new TypeToken<RouteItem>() {}.getType());
    }
}


