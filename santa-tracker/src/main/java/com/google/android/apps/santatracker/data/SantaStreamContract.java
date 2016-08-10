/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.data;

public interface SantaStreamContract {

    public static final String TABLE_NAME = "stream";

    // Fields
    public static final String COLUMN_NAME_ID = "_id"; // SQLite PK

    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";

    public static final String COLUMN_NAME_STATUS = "status";
    public static final String COLUMN_NAME_DIDYOUKNOW = "didyouknow";
    public static final String COLUMN_NAME_YOUTUBEID = "youtubeId";
    public static final String COLUMN_NAME_IMAGEURL = "imageUrl";
    public static final String COLUMN_NAME_ISNOTIFICATION = "notification";

    // Data types
    public static final String TEXT_TYPE = " TEXT";
    public static final String INT_TYPE = " INTEGER";
    public static final String REAL_TYPE = " REAL";
    public static final String COMMA_SEP = ",";

    // Boolean values
    public static final int VALUE_TRUE = 1;
    public static final int VALUE_FALSE = 0;

    // SQL statements
    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
            + TABLE_NAME + " (" + COLUMN_NAME_ID + " INTEGER PRIMARY KEY,"

            + COLUMN_NAME_TIMESTAMP + INT_TYPE + " UNIQUE " + COMMA_SEP

            + COLUMN_NAME_STATUS + TEXT_TYPE + COMMA_SEP
            + COLUMN_NAME_DIDYOUKNOW + TEXT_TYPE + COMMA_SEP
            + COLUMN_NAME_YOUTUBEID + TEXT_TYPE + COMMA_SEP
            + COLUMN_NAME_IMAGEURL + TEXT_TYPE + COMMA_SEP
            + COLUMN_NAME_ISNOTIFICATION + INT_TYPE
            + " )";

    public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS "
            + TABLE_NAME;
}
