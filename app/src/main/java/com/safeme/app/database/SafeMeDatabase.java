package com.safeme.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = {CachedJourneyEntity.class, CachedContactEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class SafeMeDatabase extends RoomDatabase {

    private static volatile SafeMeDatabase instance;

    public abstract SafeMeDao safeMeDao();

    public static SafeMeDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SafeMeDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SafeMeDatabase.class,
                            "safeme_cache.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
