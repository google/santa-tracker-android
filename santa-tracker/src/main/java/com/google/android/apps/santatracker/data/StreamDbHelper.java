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

public class StreamDbHelper extends SQLiteOpenHelper implements
        SantaStreamContract {

    public static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SantaStream.db";

    private static StreamDbHelper mInstance = null;

    private StreamDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Access to Singleton object of this class. Creates a new instance if it
     * has not been created yet.
     */
    public static StreamDbHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new StreamDbHelper(context.getApplicationContext());
        }
        return mInstance;
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

    public void emptyCardTable() {
        getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    public int getVersion() {
        return getReadableDatabase().getVersion();
    }

    /**
     * Expects a writeable {@link android.database.sqlite.SQLiteDatabase} - used for batch commits.
     */
    public void insert(SQLiteDatabase db, long timestamp, String status, String didYouKnow,
                       String imageUrl, String youtubeId, boolean isNotification) {

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_NAME_TIMESTAMP, timestamp);

        cv.put(COLUMN_NAME_STATUS, status);
        cv.put(COLUMN_NAME_DIDYOUKNOW, didYouKnow);

        cv.put(COLUMN_NAME_IMAGEURL, imageUrl);
        cv.put(COLUMN_NAME_YOUTUBEID, youtubeId);

        cv.put(COLUMN_NAME_ISNOTIFICATION, isNotification);

        // TODO: verify whether the db parameter is needed - can we just get
        // another writeable handle on the db (even if the transaction is
        // started on a different one?)
        db.insertOrThrow(TABLE_NAME, null, cv);
    }

    /**
     * Return a cursor for all cards. Parameter defines if only wear cards or only non-wear cards
     * are returned.
     */
    Cursor getAllCursor(boolean notificationOnly) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME_ISNOTIFICATION + " = "
                        + getBoolean(notificationOnly) + " ORDER BY " + COLUMN_NAME_TIMESTAMP,
                null);
    }

    /**
     * Returns a cursor for all cards following (and including) the given
     * timestamp.
     */
    public Cursor getFollowing(long time, boolean notificationOnly) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE "
                + COLUMN_NAME_TIMESTAMP + " >= " + Long.toString(time)
                + " AND " + COLUMN_NAME_ISNOTIFICATION + " = " + getBoolean(notificationOnly)
                + " ORDER BY " + COLUMN_NAME_TIMESTAMP, null);
    }

    /**
     * Returns the card with the given _id.
     */
    public StreamEntry get(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE "
                + COLUMN_NAME_ID + " = " + id, null);
        c.moveToFirst();
        StreamEntry streamEntry = getCursorEntry(c);
        c.close();

        return streamEntry;
    }

    /**
     * Helper method that converts the cursor to a card object.
     */
    static StreamEntry getCursorEntry(Cursor mCursor) {

        StreamEntry c = new StreamEntry();
        c.timestamp = mCursor.getLong(mCursor
                .getColumnIndex(COLUMN_NAME_TIMESTAMP));

        c.santaStatus = mCursor.getString(mCursor
                .getColumnIndex(COLUMN_NAME_STATUS));
        c.didYouKnow = mCursor.getString(mCursor
                .getColumnIndex(COLUMN_NAME_DIDYOUKNOW));
        c.image = mCursor.getString(mCursor
                .getColumnIndex(COLUMN_NAME_IMAGEURL));
        c.video = mCursor.getString(mCursor
                .getColumnIndex(COLUMN_NAME_YOUTUBEID));
        c.isNotification = getBoolean(mCursor.getInt(mCursor
                .getColumnIndex(COLUMN_NAME_ISNOTIFICATION)));

        return c;
    }

    private static int getBoolean(boolean isTrue) {
        return isTrue ? VALUE_TRUE : VALUE_FALSE;
    }

    private static boolean getBoolean(int value) {
        return value == VALUE_TRUE;
    }


}
