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

package com.google.android.apps.santatracker.di

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.apps.santatracker.SantaApplication
import com.google.android.apps.santatracker.tracker.di.Injectable
import dagger.android.AndroidInjection
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector

/**
 * Helper class to inject Activity/Fragment that marked as [Injectable] (or implements
 * [HasSupportFragmentInjector] for Activity) to be injected without calling
 * [AndroidInjection.inject]
 */
object AppInjector {
    private val activityCallback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            handleActivity(activity)
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }
    }

    private val fragmentManagerCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentCreated(
            fm: FragmentManager,
            fragment: Fragment,
            savedInstanceState: Bundle?
        ) {
            if (fragment is Injectable) {
                AndroidSupportInjection.inject(fragment)
            }
        }
    }

    @JvmStatic
    fun init(app: SantaApplication) {
        DaggerAppComponent.builder()
                .application(app)
                .build()
                .inject(app)

        app.registerActivityLifecycleCallbacks(activityCallback)
    }

    @JvmStatic
    private fun handleActivity(activity: Activity) {
        if (activity is Injectable || activity is HasSupportFragmentInjector) {
            AndroidInjection.inject(activity)
        }
        if (activity is FragmentActivity) {
            activity.supportFragmentManager
                    .registerFragmentLifecycleCallbacks(fragmentManagerCallback, true)
        }
    }
}
