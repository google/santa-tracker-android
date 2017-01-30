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
package com.google.android.apps.santatracker.launch;

import android.support.annotation.ColorRes;

import com.google.android.apps.santatracker.R;

/**
 * RecyclerView adapter for Santa-related activities in the village.
 */

public class SantaCardAdapter extends CardAdapter {

    public static final String KEY_SANTA_CARD = "santa_card";
    public static final String KEY_PRESENT_QUEST_CARD = "present_quest_card";

    private AbstractLaunch mLaunchSanta;
    private AbstractLaunch mLaunchPresentQuest;

    private static int sPresentCardIndex = 0;
    private static int sSantaCardIndex = 1;

    public SantaCardAdapter(SantaContext santaContext) {
        super(santaContext, new AbstractLaunch[2]);
    }

    /**
     * Change santa's flying state, allowing this adapter to re-order cards.
     * Caller must still call {@link #notifyDataSetChanged}.
     */
    public void changeState(SantaContext santaContext, boolean santaIsFlying) {
        // On christmas santa's card should be first.  Otherwise, PresentQuest should be first.
        if (santaIsFlying) {
            sSantaCardIndex = 0;
            sPresentCardIndex = 1;
        } else {
            sPresentCardIndex = 0;
            sSantaCardIndex = 1;
        };

        initializeLaunchers(santaContext);
        updateMarkerVisibility();
    }

    @Override
    public void initializeLaunchers(SantaContext santaContext) {
        if (mLaunchPresentQuest == null) {
            mLaunchPresentQuest = new LaunchPresentQuest(santaContext, this);
        }

        if (mLaunchSanta == null) {
            mLaunchSanta = new LaunchSanta(santaContext, this);
        }

        mAllLaunchers[sPresentCardIndex] = mLaunchPresentQuest;
        mAllLaunchers[sSantaCardIndex] = mLaunchSanta;
    }

    @Override
    @ColorRes
    public int getLockedViewResource(int position) {
        if (position == sSantaCardIndex) {
            return R.color.SantaTranslucentRed;
        } else {
            return R.color.SantaTranslucentGreen;
        }
    }

    public AbstractLaunch getLauncher(String key) {
        switch (key) {
            case KEY_PRESENT_QUEST_CARD:
                return mLaunchPresentQuest;
            case KEY_SANTA_CARD:
                return mLaunchSanta;
            default:
                return null;
        }
    }
}
