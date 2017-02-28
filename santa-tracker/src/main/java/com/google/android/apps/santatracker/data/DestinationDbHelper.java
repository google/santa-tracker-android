/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.apps.santatracker.service.APIProcessor;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DestinationDbHelper extends SQLiteOpenHelper implements
        SantaDestinationContract {

    public static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "SantaTracker.db";

    private static DestinationDbHelper sInstance = null;

    private DestinationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Access to Singleton object of this class. Creates a new instance if it
     * has not been created yet.
     */
    public static DestinationDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DestinationDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void reinitialise() {
        SQLiteDatabase db = getWritableDatabase();
        // delete all entries
        db.execSQL(SQL_DELETE_ENTRIES);

        onCreate(db);

        db.close();
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void emptyDestinationTable() {
        getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    public int getVersion() {
        return getReadableDatabase().getVersion();
    }

    /**
     * Expects a writeable {@link SQLiteDatabase} - used for batch commits.
     */
    public void insertDestination(SQLiteDatabase db, String id,
                                  long arrivalTime, long departureTime, String city, String region,
                                  String country, double locationLat, double locationLng,
                                  long presentsDelivered, long presentsAtDestination, long timezone, long altitude,
                                  String photos, String weather, String streetView, String gmmStreetView) {

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_NAME_IDENTIFIER, id);

        cv.put(COLUMN_NAME_ARRIVAL, arrivalTime);
        cv.put(COLUMN_NAME_DEPARTURE, departureTime);

        cv.put(COLUMN_NAME_CITY, city);
        cv.put(COLUMN_NAME_REGION, region);
        cv.put(COLUMN_NAME_COUNTRY, country);

        cv.put(COLUMN_NAME_LAT, locationLat);
        cv.put(COLUMN_NAME_LNG, locationLng);

        cv.put(COLUMN_NAME_PRESENTSDELIVERED, presentsDelivered);
        cv.put(COLUMN_NAME_PRESENTS_DESTINATION, presentsAtDestination);

        cv.put(COLUMN_NAME_TIMEZONE, timezone);
        cv.put(COLUMN_NAME_ALTITUDE, altitude);
        cv.put(COLUMN_NAME_PHOTOS, photos);
        cv.put(COLUMN_NAME_WEATHER, weather);
        cv.put(COLUMN_NAME_STREETVIEW, streetView);
        cv.put(COLUMN_NAME_GMMSTREETVIEW, gmmStreetView);

        // TODO: verify whether the db parameter is needed - can we just get
        // another writeable handle on the db (even if the transaction is
        // started on a different one?)
        db.insertOrThrow(TABLE_NAME, null, cv);
    }

    Cursor getAllDestinationCursor() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY "
                + COLUMN_NAME_ARRIVAL, null);
    }

    public long getFirstDeparture() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COLUMN_NAME_DEPARTURE + " FROM "
                + TABLE_NAME + " ORDER BY " + COLUMN_NAME_DEPARTURE
                + " ASC LIMIT 1", null);
        c.moveToFirst();
        long l;
        if (c.isAfterLast()) {
            l = -1;
        } else {
            l = c.getLong(c.getColumnIndex(COLUMN_NAME_DEPARTURE));
        }
        c.close();
        return l;
    }

    public long getLastArrival() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COLUMN_NAME_ARRIVAL + " FROM "
                + TABLE_NAME + " ORDER BY " + COLUMN_NAME_ARRIVAL
                + " DESC LIMIT 1", null);
        c.moveToFirst();
        long l;
        if (c.isAfterLast()) {
            l = -1;
        } else {
            l = c.getLong(c.getColumnIndex(COLUMN_NAME_ARRIVAL));
        }
        c.close();
        return l;
    }

    public long getLastDeparture() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COLUMN_NAME_DEPARTURE + " FROM "
                + TABLE_NAME + " ORDER BY " + COLUMN_NAME_DEPARTURE
                + " DESC LIMIT 1", null);
        c.moveToFirst();
        long l;
        if (c.isAfterLast()) {
            l = -1;
        } else {
            l = c.getLong(c.getColumnIndex(COLUMN_NAME_DEPARTURE));
        }
        c.close();
        return l;
    }

    /**
     * Returns the destination with the given _id.
     */
    public Destination getDestination(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE "
                + COLUMN_NAME_ID + " = " + id, null);
        c.moveToFirst();
        Destination d = getCursorDestination(c);
        c.close();

        return d;
    }

    /**
     * Helper method that converts the cursor to a destination object.
     */
    static Destination getCursorDestination(Cursor mCursor) {

        Destination d = new Destination();

        d.id = mCursor.getInt(mCursor
                .getColumnIndex(SantaDestinationContract.COLUMN_NAME_ID));
        d.identifier = mCursor
                .getString(mCursor
                        .getColumnIndex(SantaDestinationContract.COLUMN_NAME_IDENTIFIER));

        d.city = mCursor.getString(mCursor
                .getColumnIndex(SantaDestinationContract.COLUMN_NAME_CITY));
        d.region = mCursor.getString(mCursor
                .getColumnIndex(SantaDestinationContract.COLUMN_NAME_REGION));
        d.country = mCursor.getString(mCursor
                .getColumnIndex(SantaDestinationContract.COLUMN_NAME_COUNTRY));

        d.arrival = mCursor.getLong(mCursor
                .getColumnIndex(SantaDestinationContract.COLUMN_NAME_ARRIVAL));
        d.departure = mCursor
                .getLong(mCursor
                        .getColumnIndex(SantaDestinationContract.COLUMN_NAME_DEPARTURE));

        double lat = mCursor.getDouble(mCursor
                .getColumnIndex(SantaDestinationContract.COLUMN_NAME_LAT));
        double lng = mCursor.getDouble(mCursor
                .getColumnIndex(SantaDestinationContract.COLUMN_NAME_LNG));
        d.position = new LatLng(lat, lng);

        d.presentsDelivered = mCursor
                .getLong(mCursor
                        .getColumnIndex(SantaDestinationContract.COLUMN_NAME_PRESENTSDELIVERED));
        d.presentsDeliveredAtDestination = mCursor
                .getLong(mCursor
                        .getColumnIndex(SantaDestinationContract.COLUMN_NAME_PRESENTS_DESTINATION));

        d.timezone = mCursor
                .getLong(mCursor.getColumnIndex(SantaDestinationContract.COLUMN_NAME_TIMEZONE));
        d.altitude = mCursor
                .getLong(mCursor.getColumnIndex(SantaDestinationContract.COLUMN_NAME_ALTITUDE));
        d.photoString = mCursor
                .getString(mCursor.getColumnIndex(SantaDestinationContract.COLUMN_NAME_PHOTOS));
        d.weatherString = mCursor
                .getString(mCursor.getColumnIndex(SantaDestinationContract.COLUMN_NAME_WEATHER));
        d.streetViewString = mCursor
                .getString(mCursor.getColumnIndex(SantaDestinationContract.COLUMN_NAME_STREETVIEW));

        d.gmmStreetViewString = mCursor
                .getString(
                        mCursor.getColumnIndex(SantaDestinationContract.COLUMN_NAME_GMMSTREETVIEW));

        // Process the panoramio string if possible
        d.photos = processPhoto(d.photoString);
        d.weather = processWeather(d.weatherString);
        d.streetView = processStreetView(d.streetViewString);
        d.gmmStreetView = processStreetView(d.gmmStreetViewString);

        return d;
    }


    private static Destination.Photo[] processPhoto(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        ArrayList<Destination.Photo> list = new ArrayList<>(5);

        try {
            JSONArray array = new JSONArray(s);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                Destination.Photo photo = new Destination.Photo();
                photo.url = json.getString(APIProcessor.FIELD_PHOTO_URL);
                photo.attributionHTML = json.getString(APIProcessor.FIELD_PHOTO_ATTRIBUTIONHTML);

                list.add(photo);
            }

        } catch (JSONException e) {
            // ignore invalid values
        }
        return list.isEmpty() ? null : list.toArray(new Destination.Photo[list.size()]);

    }

    private static Destination.StreetView processStreetView(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        try {
            Destination.StreetView streetView = new Destination.StreetView();
            JSONObject json = new JSONObject(s);
            streetView.id = json.getString(APIProcessor.FIELD_STREETVIEW_ID);
            streetView.heading = json.getDouble(APIProcessor.FIELD_STREETVIEW_HEADING);

            if (json.has(APIProcessor.FIELD_STREETVIEW_LATITUDE) &&
                    json.has(APIProcessor.FIELD_STREETVIEW_LONGITUDE)) {
                double lat = json.getDouble(APIProcessor.FIELD_STREETVIEW_LATITUDE);
                double lng = json.getDouble(APIProcessor.FIELD_STREETVIEW_LONGITUDE);
                streetView.position = new LatLng(lat, lng);
            }
            return streetView;
        } catch (JSONException e) {
            // ignore invalid values
        }
        return null;
    }

    private static Destination.Weather processWeather(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        try {
            Destination.Weather weather = new Destination.Weather();
            JSONObject json = new JSONObject(s);
            weather.url = APIProcessor.getExistingJSONString(json, APIProcessor.FIELD_WEATHER_URL);
            weather.tempC = APIProcessor
                    .getExistingJSONDouble(json, APIProcessor.FIELD_WEATHER_TEMPC);
            weather.tempF = APIProcessor.getExistingJSONDouble(json,
                    APIProcessor.FIELD_WEATHER_TEMPF);

            return weather;
        } catch (JSONException e) {
            // ignore invalid values
        }
        return null;
    }


}
