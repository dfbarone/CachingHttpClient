package com.dfbarone.cachinghttp.persistence.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Created by dominicbarone on 11/6/17.
 */

@Database(entities = HttpResponseEntry.class, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static String DATABASE_NAME = "com.dfbarone.cachinghttp.persistence.db";
    private static AppDatabase INSTANCE;

    public abstract HttpResponseDao httpResponseDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                            //.allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
