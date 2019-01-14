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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import com.google.android.apps.santatracker.R

/** Score board with optional scores, time and level.  */
class ScoreTitleBar(parent: ViewGroup) {

    private val backgroundView: View = parent.findViewById(R.id.scorebar_background)

    private val scorebarMuteButton: View = parent.findViewById(R.id.scorebar_mute_button)
    private val overlayMuteButton: View = parent.findViewById(R.id.overlay_mute_button)

    private val scoreText: TextView = parent.findViewById(R.id.scorebar_score)
    private val timeText: TextView = parent.findViewById(R.id.scorebar_time)
    private val levelText: TextView = parent.findViewById(R.id.scorebar_level)

    private val scoreGroup: Group = parent.findViewById(R.id.group_score)
    private val timeGroup: Group = parent.findViewById(R.id.group_time)
    private val levelGroup: Group = parent.findViewById(R.id.group_level)

    init {
        levelGroup.visibility = View.GONE
    }

    fun setUi(
        score: Int = Int.MIN_VALUE,
        time: Int = Int.MIN_VALUE,
        level: Int = Int.MIN_VALUE,
        maxLevel: Int = Int.MIN_VALUE
    ) {
        setScore(score)
        setTime(time)
        setLevel(level, maxLevel)

        backgroundView.isVisible = scoreGroup.isVisible || timeGroup.isVisible ||
                levelGroup.isVisible

        scorebarMuteButton.isVisible = backgroundView.isVisible
        overlayMuteButton.isVisible = !backgroundView.isVisible
    }

    private fun setLevel(level: Int, maxLevel: Int) {
        if (level > 0) {
            levelGroup.visibility = View.VISIBLE

            levelText.text = buildSpannedString {
                append(level.toString())
                if (maxLevel > 0) {
                    color(0x90FFFFFF.toInt()) {
                        append("\u00A0")
                        append("•")
                        append("\u00A0")
                        append(maxLevel.toString())
                    }
                }
            }
        } else {
            levelGroup.visibility = View.GONE
        }
    }

    private fun setScore(score: Int = Int.MIN_VALUE) {
        if (score > 0) {
            scoreText.text = String.format("%,d", score)
            scoreGroup.visibility = View.VISIBLE
        } else {
            scoreGroup.visibility = View.GONE
        }
    }

    private fun setTime(time: Int = Int.MIN_VALUE) {
        if (time > 0) {
            val minutes = time / 60
            val seconds = time % 60
            timeText.text = String.format("%02d:%02d", minutes, seconds)
            timeGroup.visibility = View.VISIBLE
        } else {
            timeGroup.visibility = View.GONE
        }
    }
}
