package com.example.ajp.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SavedRouteEntity.class, JourneyLog.class}, version = 4, exportSchema = false)
@TypeConverters(Converters.class)
/**
 * Room database holder and migration registration point.
 */
public abstract class AppDatabase extends androidx.room.RoomDatabase {

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        // Handles a focused part of this feature flow and keeps related logic encapsulated.
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("DELETE FROM journey_logs");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public abstract SavedRouteDao savedRouteDao();
    public abstract JourneyLogDao journeyLogDao();

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


