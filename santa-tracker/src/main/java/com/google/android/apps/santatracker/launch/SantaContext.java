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

package com.google.android.apps.santatracker.launch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;

/** Interface for Context holder and convenience methods for AbstractLaunch objects. */
public interface SantaContext {

    Context getActivityContext();

    Context getApplicationContext();

    Resources getResources();

    void launchActivity(Intent intent);

    void launchActivity(Intent intent, @NonNull ActivityOptionsCompat options);

    void launchActivityDelayed(Intent intent, View v);

    void launchActivityDelayed(Intent intent, View v, @NonNull ActivityOptionsCompat options);

    /** Needed for shared element transitions. */
    Activity getActivity();
}
