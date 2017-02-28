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

package com.google.android.apps.santatracker.rocketsleigh;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Handy utility class for supporting TV.
 */
public class TvUtil {

    // Check whether this app is running on TV or not.
    public static boolean isTv(Context context) {

        final UiModeManager
                manager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        return manager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}
