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

import android.content.Intent;
import android.view.View;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.doodles.PineappleActivity;
import com.google.android.apps.santatracker.doodles.shared.LaunchDecisionMaker;

/**
 * Launcher to launch one of the mini games.
 */
public class LaunchDoodle extends AbstractLaunch {

    private String mGameKey;
    private String mName;

    /**
     * @param gameKey constant representing game to launch, such as
     *        {@link LaunchDecisionMaker#SWIMMING_GAME_VALUE}.
     * See {@link AbstractLaunch} for other parameters.
     */
    public LaunchDoodle(SantaContext context, LauncherDataChangedCallback adapter,
                        String gameKey, int contentDescriptionId, int cardDrawable) {
        super(context, adapter, contentDescriptionId, cardDrawable);

        this.mName = context.getResources().getString(contentDescriptionId);
        this.mGameKey = gameKey;
    }

    @Override
    public void onClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                Intent intent = new Intent(mContext.getActivityContext(), PineappleActivity.class);
                intent.putExtra(LaunchDecisionMaker.START_GAME_KEY, mGameKey);

                mContext.launchActivityDelayed(intent, v);
                break;
            case STATE_DISABLED:
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(),
                        R.string.generic_game_disabled, mName);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getApplicationContext(), mName);
                break;
            case STATE_DISABLED:
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(),
                        R.string.generic_game_disabled,
                        mName);
                break;
        }
        return true;
    }
}
