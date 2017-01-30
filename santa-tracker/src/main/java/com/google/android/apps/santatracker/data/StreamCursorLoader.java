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

import android.content.Context;
import android.database.Cursor;

/**
 * Loader that returns a Cursor from
 * {@link com.google.android.apps.santatracker.data.StreamDbHelper#getFollowing(long, boolean)}
 *
 */
public class StreamCursorLoader extends SqliteCursorLoader {

    private boolean mNotificationOnly = false;

    public StreamCursorLoader(Context context, boolean notificationOnly) {
        super(context);
        mNotificationOnly = notificationOnly;
    }

    @Override
    public Cursor getCursor() {
        return StreamDbHelper.getInstance(getContext()).getAllCursor(mNotificationOnly);
    }

}
