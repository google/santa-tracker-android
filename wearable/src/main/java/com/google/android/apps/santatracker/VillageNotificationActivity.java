/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker;

import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.village.Village;
import com.google.android.apps.santatracker.village.VillageView;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.wearable.view.CardScrollView;
import android.view.Gravity;
import android.widget.TextView;

public class VillageNotificationActivity extends FragmentActivity {
    private static String VILLAGE_TAG = "VillageFragment";

    private TextView mTextView;
    private Village mVillage;
    private VillageView mVillageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_takeoff);

        mVillageView = (VillageView) findViewById(R.id.villageView);
        mVillage = (Village) getSupportFragmentManager().findFragmentByTag(VILLAGE_TAG);
        if (mVillage == null) {
            mVillage = new Village();
            getSupportFragmentManager().beginTransaction().add(mVillage, VILLAGE_TAG).commit();
        }
        mVillageView.setVillage(mVillage);

        //Align card to bottom if content is smaller than screen.
        CardScrollView cardScrollView =
                (CardScrollView) findViewById(R.id.card_scroll_view);
        cardScrollView.setCardGravity(Gravity.BOTTOM);

        mTextView = (TextView) findViewById(R.id.text);
        mTextView.setTypeface(Typeface.createFromAsset(getAssets(),
                getResources().getString(R.string.typeface_robotocondensed_light)));
        mTextView.setText(getIntent().getStringExtra(NotificationConstants.KEY_CONTENT));
    }
}
