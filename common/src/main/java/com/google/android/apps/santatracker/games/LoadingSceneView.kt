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

package com.google.android.apps.santatracker.games

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.google.android.apps.santatracker.common.R

/** Animated loading screen with progress indicator.  */
class LoadingSceneView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {

        // Inflate custom layout
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.layout_loading_screen, this, true)

        this.gravity = Gravity.CENTER_VERTICAL
        this.setBackgroundColor(resources.getColor(R.color.loading_web_background_blue))
        this.orientation = LinearLayout.VERTICAL

        val progressBar = findViewById<ProgressBar>(R.id.progressbar)
        if (Build.VERSION.SDK_INT >= 24) {
            progressBar.indeterminateDrawable = resources.getDrawable(R.drawable.avd_loading_bar, context.theme)
        } else {
            progressBar.indeterminateTintList = ColorStateList.valueOf(resources.getColor(R.color.SantaWhite))
        }
    }
}
