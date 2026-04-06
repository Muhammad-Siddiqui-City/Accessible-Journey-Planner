package com.example.ajp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
/**
 * DAO contract for SavedRoute persistence operations.
 * Declares query and mutation operations that Room implements at compile time.
 * Keeping SQL declarations here makes read/write intent explicit and searchable.
 */

public interface SavedRouteDao {

    @Insert
    long insert(SavedRouteEntity entity);

    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    LiveData<List<SavedRouteEntity>> getAll();

    @Delete
    void delete(SavedRouteEntity entity);
}

