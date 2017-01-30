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
import com.google.android.apps.santatracker.games.SplashActivity;
import com.google.fpl.pie_noon.FPLActivity;
import com.google.fpl.pie_noon.FPLTvActivity;

/**
 * Launches Snowdown (FKA Pie Noon).
 */
public class LaunchSnowdown extends AbstractLaunch {

    private final Class mLaunchActivityClass;

    public LaunchSnowdown(SantaContext context, LauncherDataChangedCallback adapter) {
        super(context, adapter, R.string.snowdown, R.drawable.android_game_cards_snowdown);


        if (isTV()) {
            mLaunchActivityClass = FPLTvActivity.class;
        } else {
            mLaunchActivityClass = FPLActivity.class;
        }
    }

    static public int getId() {
        return R.string.snowdown;
    }

    @Override
    public void onClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                Intent intent = SplashActivity.getIntent(mContext.getActivityContext(),
                        R.drawable.android_game_cards_snowdown,
                        R.string.snowdown,
                        true /* landscape */,
                        mLaunchActivityClass);
                mContext.launchActivity(intent);
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), getDisabledString(R.string.snowdown));
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), R.string.snowdown_locked);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getApplicationContext(), R.string.snowdown);
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), getDisabledString(R.string.snowdown));
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), R.string.snowdown_locked);
                break;
        }
        return true;
    }

    @Override
    public boolean handleVoiceAction(Intent intent) {
        return clickIfMatchesDescription(intent, VoiceAction.ACTION_PLAY_GAME,
                VoiceAction.ACTION_PLAY_GAME_EXTRA);
    }

}
