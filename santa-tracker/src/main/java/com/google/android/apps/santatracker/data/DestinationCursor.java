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

import android.database.Cursor;

/**
 * Encapsulates a cursor of Destinations.
 *
 */
public class DestinationCursor extends CursorHelper<Destination> implements SantaDestinationContract {

    public DestinationCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Returns the {@link Destination} object of the position of the current
     * cursor position. If the cursor points at an empty position, a
     * {@link Destination} object with undefined values is returned. (The
     * calling method should verify that the given Cursor is at a valid
     * position.
     */
    protected Destination getParsedObject() {
        return DestinationDbHelper.getCursorDestination(mCursor);
    }

    /**
     * Returns true if there are no destinations left.
     */
    public boolean isFinished() {
        return mCursor.isAfterLast();
    }

    /**
     * Returns true if the Checks whether the departure time of the current
     * position is in the past.
     */
    public boolean isInPast(long time) {
        return time > mCursor.getLong(mCursor
                .getColumnIndex(COLUMN_NAME_DEPARTURE));
    }

    /**
     * Returns true if the given time is between the departure and arrival times
     * of the current position
     */
    public boolean isVisiting(long time) {
        //noinspection SimplifiableIfStatement
        if (mCursor.isAfterLast()) {
            return false; // already finished
        }

        return time >= mCursor.getLong(mCursor
                .getColumnIndex(COLUMN_NAME_ARRIVAL))
                && time <= mCursor.getLong(mCursor
                .getColumnIndex(COLUMN_NAME_DEPARTURE));
    }

    public boolean moveToNext() {
        return mCursor.moveToNext();
    }

}
