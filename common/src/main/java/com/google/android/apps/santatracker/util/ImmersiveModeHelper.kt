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

package com.google.android.apps.santatracker.util

import android.annotation.TargetApi
import android.os.Build
import android.view.View
import android.view.Window

object ImmersiveModeHelper {

    private val TAG = ImmersiveModeHelper::class.java.simpleName

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @JvmStatic
    fun installSystemUiVisibilityChangeListener(w: Window) {
        val view = w.decorView
        view.setOnSystemUiVisibilityChangeListener { visibility ->
            SantaLog.d(
                    TAG,
                    "setOnSystemUiVisibilityChangeListener: visibility=$visibility")
            setImmersiveSticky(w)
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @JvmStatic
    fun setImmersiveSticky(w: Window) {
        w.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @JvmStatic
    fun setImmersiveStickyWithActionBar(w: Window) {
        w.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}
