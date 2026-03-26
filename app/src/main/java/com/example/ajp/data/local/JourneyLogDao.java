package com.example.ajp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;







@Dao
/**
 * Room DAO for JourneyLog persistence operations.
 */
public interface JourneyLogDao {

    @Insert
    long insert(JourneyLog log);

    @Query("SELECT * FROM journey_logs WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    List<JourneyLog> getLogsSince(long startTime);
}

