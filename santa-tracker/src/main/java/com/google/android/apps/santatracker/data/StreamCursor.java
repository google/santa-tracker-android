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
 * Encapsulates a cursor of Cards.
 */
public class StreamCursor extends CursorHelper<StreamEntry> implements SantaStreamContract {

    public StreamCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Returns the {@link StreamEntry} object of the position of the
     * current
     * cursor position. If the cursor points at an empty position, a
     * {@link StreamEntry} object with undefined values is returned.
     * (The
     * calling method should verify that the given Cursor is at a valid
     * position.
     */
    @Override
    protected StreamEntry getParsedObject() {
        return StreamDbHelper.getCursorEntry(mCursor);
    }

}
