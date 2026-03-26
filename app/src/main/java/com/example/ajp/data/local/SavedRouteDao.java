package com.example.ajp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;







@Dao
/**
 * Room DAO for SavedRoute persistence operations.
 */
public interface SavedRouteDao {

    @Insert
    long insert(SavedRouteEntity entity);

    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    LiveData<List<SavedRouteEntity>> getAll();

    @Delete
    void delete(SavedRouteEntity entity);
}

