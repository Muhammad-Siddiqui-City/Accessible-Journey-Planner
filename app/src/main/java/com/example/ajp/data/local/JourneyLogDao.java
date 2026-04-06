package com.example.ajp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
/**
 * DAO contract for JourneyLog persistence operations.
 * Declares query and mutation operations that Room implements at compile time.
 * Keeping SQL declarations here makes read/write intent explicit and searchable.
 */

public interface JourneyLogDao {

    @Insert
    long insert(JourneyLog log);

    @Query("SELECT * FROM journey_logs WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    List<JourneyLog> getLogsSince(long startTime);
}

