/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.doodles.penguinswim;

import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.SWIMMING_GAME_TYPE;

import android.app.Fragment;
import com.google.android.apps.santatracker.doodles.BaseDoodleActivity;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleDebugLogger;

public class PenguinSwimActivity extends BaseDoodleActivity {
    @Override
    protected String getGameType() {
        return SWIMMING_GAME_TYPE;
    }

    @Override
    protected int getAnalyticsStringResource() {
        return R.string.analytics_screen_swimming;
    }

    @Override
    protected Fragment makeFragment(DoodleDebugLogger logger) {
        return SwimmingFragment.newInstance(false);
    }
}
