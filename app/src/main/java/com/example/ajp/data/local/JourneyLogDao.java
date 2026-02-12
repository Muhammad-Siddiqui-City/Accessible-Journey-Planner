package com.example.ajp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

/**
 * DAO for journey logs. Add in Commit 4.
 * PURPOSE: insert(log), getLogsSince(startTime) for analytics time window.
 * WHY: Analytics needs logs in last 7 days; ORDER BY timestamp DESC.
 * ISSUES: None.
 */
@Dao
public interface JourneyLogDao {

    @Insert
    long insert(JourneyLog log);

    @Query("SELECT * FROM journey_logs WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    List<JourneyLog> getLogsSince(long startTime);
}
