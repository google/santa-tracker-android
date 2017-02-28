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

package com.google.android.apps.santatracker.dasherdancer;

import com.google.android.apps.santatracker.util.AnalyticsManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class CharacterActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_character);

        AnalyticsManager.initializeAnalyticsTracker(this);
        AnalyticsManager.sendScreenView(R.string.analytics_screen_dasher_charselect);
    }

    public void onCharacterClick(View view) {
        int characterId = -1;
        if (view.getId() == R.id.btn_character_santa) {
            characterId = DasherDancerActivity.CHARACTER_ID_SANTA;
        } else if (view.getId() == R.id.btn_character_elf) {
            characterId = DasherDancerActivity.CHARACTER_ID_ELF;
        } else if (view.getId() == R.id.btn_character_reindeer) {
            characterId = DasherDancerActivity.CHARACTER_ID_REINDEER;
        } else if (view.getId() == R.id.btn_character_snowman) {
            characterId = DasherDancerActivity.CHARACTER_ID_SNOWMAN;
        }
        Intent result = new Intent();
        result.putExtra(DasherDancerActivity.EXTRA_CHARACTER_ID, characterId);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

}
