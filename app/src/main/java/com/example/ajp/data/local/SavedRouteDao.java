package com.example.ajp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

/**
 * DAO for saved routes. Add in Commit 4.
 * PURPOSE: Insert, getAll (LiveData, newest first), delete.
 * WHY: Used by route-details screen to load/save/delete saved routes.
 * ISSUES: None.
 */
@Dao
public interface SavedRouteDao {

    @Insert
    long insert(SavedRouteEntity entity);

    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    LiveData<List<SavedRouteEntity>> getAll();

    @Delete
    void delete(SavedRouteEntity entity);
}
