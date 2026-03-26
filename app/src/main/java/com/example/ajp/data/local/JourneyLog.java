package com.example.ajp.data.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;







@Entity(tableName = "journey_logs")
/**
 * Room entity backing the JourneyLog table.
 */
public class JourneyLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long timestamp;
    public int durationMinutes;
    public int savedMinutes;
    public String mode;
    public boolean isCrowdLow;
    public String fromStation;
    public String toStation;

    public JourneyLog() { }

    @Ignore
    public JourneyLog(long timestamp, int durationMinutes, int savedMinutes, String mode, boolean isCrowdLow, String fromStation, String toStation) {
        this.timestamp = timestamp;
        this.durationMinutes = durationMinutes;
        this.savedMinutes = savedMinutes;
        this.mode = mode != null ? mode : "Mixed";
        this.isCrowdLow = isCrowdLow;
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
    }
}

