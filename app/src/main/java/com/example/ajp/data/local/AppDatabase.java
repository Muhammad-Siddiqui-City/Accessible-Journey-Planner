package com.example.ajp.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room database. Add in Commit 4 with entities and DAOs.
 * Holds saved routes and journey logs; singleton; TypeConverters for JSON columns.
 */
@Database(entities = {SavedRouteEntity.class, JourneyLog.class}, version = 4, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends androidx.room.RoomDatabase {

    /* --- BLOCK: Migration 3→4 ---
     * PURPOSE: Clear journey_logs when schema or usage changed.
     * WHY: Avoid invalid data after a breaking change; migration runs once per upgrade.
     * ISSUES: exportSchema = false so no schema export; for production consider exportSchema = true.
     */
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("DELETE FROM journey_logs");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public abstract SavedRouteDao savedRouteDao();
    public abstract JourneyLogDao journeyLogDao();

    /* --- BLOCK: Singleton getInstance ---
     * PURPOSE: One database instance app-wide; create on first use.
     * WHY: Room recommends one DB instance; use applicationContext to avoid leaks.
     * ISSUES: fallbackToDestructiveMigration() wipes DB on unknown migrations; ok for prototype.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "ajp_database"
                    ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
