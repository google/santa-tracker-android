/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.presentquest.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.android.apps.santatracker.presentquest.vo.Place;
import com.google.android.apps.santatracker.presentquest.vo.Present;
import com.google.android.apps.santatracker.presentquest.vo.User;
import com.google.android.apps.santatracker.presentquest.vo.Workshop;
import com.google.android.apps.santatracker.util.SantaLog;

@Database(
        entities = {User.class, Present.class, Place.class, Workshop.class},
        version = 1)
public abstract class PQDatabase extends RoomDatabase {
    private static final String LOG_TAG = PQDatabase.class.getSimpleName();
    public static final String DATABASE_NAME = "present-quest";

    public abstract UserDao userDao();

    public abstract PresentDao presentDao();

    public abstract PlaceDao placeDao();

    public abstract WorkshopDao workshopDao();

    // For Singleton instantiation
    private static final Object LOCK = new Object();
    private static PQDatabase database;

    public static PQDatabase getInstance(Context context) {
        SantaLog.d(LOG_TAG, "Getting the database");
        if (database == null) {
            synchronized (LOCK) {
                database =
                        Room.databaseBuilder(
                                        context.getApplicationContext(),
                                        PQDatabase.class,
                                        PQDatabase.DATABASE_NAME)
                                .allowMainThreadQueries()
                                .build();
                // TODO switch off main thread
                SantaLog.d(LOG_TAG, "Made new database");
            }
        }
        return database;
    }
}
