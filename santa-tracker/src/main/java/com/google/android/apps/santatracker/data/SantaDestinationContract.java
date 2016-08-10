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

public interface SantaDestinationContract {

    public static final String TABLE_NAME = "destinations";

    // Fields
    public static final String COLUMN_NAME_ID = "_id"; // SQLite PK
    public static final String COLUMN_NAME_IDENTIFIER = "identifier"; // Identifier
    // of
    // location

    public static final String COLUMN_NAME_ARRIVAL = "arrival";
    public static final String COLUMN_NAME_DEPARTURE = "departure";

    public static final String COLUMN_NAME_CITY = "city";
    public static final String COLUMN_NAME_REGION = "region";
    public static final String COLUMN_NAME_COUNTRY = "country";

    public static final String COLUMN_NAME_LAT = "lat";
    public static final String COLUMN_NAME_LNG = "lng";

    public static final String COLUMN_NAME_PRESENTSDELIVERED = "presentsdelivered";
    public static final String COLUMN_NAME_PRESENTS_DESTINATION = "presentsdeliveredatdestination";

    public static final String COLUMN_NAME_TIMEZONE = "timezone";
    public static final String COLUMN_NAME_ALTITUDE = "altitude";
    public static final String COLUMN_NAME_PHOTOS = "photos";
    public static final String COLUMN_NAME_WEATHER = "weather";
    public static final String COLUMN_NAME_STREETVIEW = "streetview";
    public static final String COLUMN_NAME_GMMSTREETVIEW = "gmmstreetview";


    // Data types
    public static final String TEXT_TYPE = " TEXT";
    public static final String INT_TYPE = " INTEGER";
    public static final String REAL_TYPE = " REAL";
    public static final String COMMA_SEP = ",";

    // SQL statements
    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
            + TABLE_NAME + " (" + COLUMN_NAME_ID + " INTEGER PRIMARY KEY,"

            + COLUMN_NAME_IDENTIFIER + TEXT_TYPE + " UNIQUE " + COMMA_SEP

            + COLUMN_NAME_ARRIVAL + INT_TYPE + COMMA_SEP
            + COLUMN_NAME_DEPARTURE + INT_TYPE + COMMA_SEP

            + COLUMN_NAME_CITY + TEXT_TYPE + COMMA_SEP + COLUMN_NAME_REGION
            + TEXT_TYPE + COMMA_SEP + COLUMN_NAME_COUNTRY + TEXT_TYPE
            + COMMA_SEP

            + COLUMN_NAME_LAT + REAL_TYPE + COMMA_SEP + COLUMN_NAME_LNG
            + REAL_TYPE + COMMA_SEP

            + COLUMN_NAME_PRESENTSDELIVERED + INT_TYPE + COMMA_SEP
            + COLUMN_NAME_PRESENTS_DESTINATION + INT_TYPE + COMMA_SEP

            + COLUMN_NAME_TIMEZONE + INT_TYPE + COMMA_SEP
            + COLUMN_NAME_ALTITUDE + INT_TYPE + COMMA_SEP
            + COLUMN_NAME_PHOTOS + TEXT_TYPE + COMMA_SEP
            + COLUMN_NAME_WEATHER + TEXT_TYPE + COMMA_SEP
            + COLUMN_NAME_STREETVIEW + TEXT_TYPE + COMMA_SEP
            + COLUMN_NAME_GMMSTREETVIEW + TEXT_TYPE
            + " )";

    public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS "
            + TABLE_NAME;
}
