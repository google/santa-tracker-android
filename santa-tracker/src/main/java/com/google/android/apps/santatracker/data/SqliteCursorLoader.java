/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.data;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

/**
 * A loader that queries the {@link DestinationDbHelper} and returns a
 * {@link Cursor}. This class implements the {@link Loader} protocol in a
 * standard way for querying cursors, building on {@link AsyncTaskLoader} to
 * perform the cursor query on a background thread so that it does not block the
 * application's UI.
 * <p>
 * The abstract method {@link #getCursor()} needs to be overridden to return the
 * desired {@link Cursor}.
 * <p>
 * Note: This implementation is copied from the AOSP
 * {@link android.content.CursorLoader} class and modified to directly interact
 * with the {@link SQLiteDatabase} held by {@link DestinationDbHelper}. See
 * hackbod's statement at:
 * https://groups.google.com/d/msg/android-developers/J-Uql3Mn73Y/3haYPQ-pR7sJ
 */
abstract class SqliteCursorLoader extends AsyncTaskLoader<Cursor> {

    private final ForceLoadContentObserver mObserver;

    private Cursor mCursor;

    /**
     * Returns the Cursor that is to be loaded.
     */
    public abstract Cursor getCursor();

    @Override
    public Cursor loadInBackground() {
        // TODO: check if call to new DestinationDBHelper is appropriate here or
        // whether we can do it in the constructor and keep a handle
        Cursor cursor = getCursor();

        if (cursor != null) {
            // Ensure the cursor window is filled
            cursor.getCount();
            registerContentObserver(cursor, mObserver);
        }
        return cursor;
    }

    /**
     * Registers an observer to get notifications from the content provider when
     * the cursor needs to be refreshed.
     */
    private void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(observer);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    SqliteCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is
     * ready the callbacks will be called on the UI thread. If a previous load
     * has been completed and is still valid the result may be passed to the
     * callbacks immediately.
     * <p>
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

}