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

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import androidx.test.espresso.IdlingResource;
import java.util.List;

/** Idling resource for Espresso tests to check if the Splash screen is running. */
public class SplashIdlingResource implements IdlingResource {

    private Context mContext;
    private ResourceCallback mResourceCallback;

    SplashIdlingResource(Context context) {
        mContext = context;
    }

    @Override
    public String getName() {
        return "Idling<SplashActivity>";
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = !isSplashActivityRunning();
        if (idle && mResourceCallback != null) {
            mResourceCallback.onTransitionToIdle();
        }

        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.mResourceCallback = callback;
    }

    private boolean isSplashActivityRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);

        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getClassName().contains("SplashActivity");
    }
}
