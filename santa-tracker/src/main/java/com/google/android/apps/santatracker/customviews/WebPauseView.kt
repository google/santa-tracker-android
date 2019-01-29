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

package com.google.android.apps.santatracker.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.apps.santatracker.R

class WebPauseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var clickListener: Listener? = null

    private var resumeButton: View
    private var playAgainButton: View
    private var backToVillageButton: View

    init {
        View.inflate(context, R.layout.layout_web_pause, this)
        setBackgroundColor(ContextCompat.getColor(context, R.color.end_game_background_blue))

        resumeButton = findViewById(R.id.resume_button)
        playAgainButton = findViewById(R.id.playagain_button)
        backToVillageButton = findViewById(R.id.backtovillage_button)

        resumeButton.setOnClickListener { clickListener?.onResumeClick() }
        playAgainButton.setOnClickListener { clickListener?.onPlayAgainClick() }
        backToVillageButton.setOnClickListener { clickListener?.onGoToVillageClick() }
    }

    interface Listener {
        fun onResumeClick()
        fun onPlayAgainClick()
        fun onGoToVillageClick()
    }
}