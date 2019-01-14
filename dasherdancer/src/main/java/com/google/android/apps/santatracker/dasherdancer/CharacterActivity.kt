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

package com.google.android.apps.santatracker.dasherdancer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.firebase.analytics.FirebaseAnalytics

class CharacterActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character)

        MeasurementManager.recordScreenView(
                FirebaseAnalytics.getInstance(this),
                getString(R.string.analytics_screen_dasher_charselect))
    }

    fun onCharacterClick(view: View) {
        var characterId = -1
        when (view.id) {
            R.id.btn_character_santa -> characterId = DasherDancerActivity.CHARACTER_ID_SANTA
            R.id.btn_character_elf -> characterId = DasherDancerActivity.CHARACTER_ID_ELF
            R.id.btn_character_reindeer -> characterId = DasherDancerActivity.CHARACTER_ID_REINDEER
            R.id.btn_character_snowman -> characterId = DasherDancerActivity.CHARACTER_ID_SNOWMAN
        }
        val result = Intent()
        result.putExtra(DasherDancerActivity.EXTRA_CHARACTER_ID, characterId)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
