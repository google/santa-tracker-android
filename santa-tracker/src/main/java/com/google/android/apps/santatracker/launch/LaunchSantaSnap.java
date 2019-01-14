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
package com.google.android.apps.santatracker.launch;

import android.content.Intent;
import android.view.View;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.SplashActivity;

/** Launcher to launch the Santa Snap game. */
public class LaunchSantaSnap extends AbstractFeatureModuleLaunch {

    /**
     * Constructs a new Santa Snap launch (marker).
     *
     * @param context The application (Santa) context
     */
    public LaunchSantaSnap(SantaContext context, LauncherDataChangedCallback adapter) {
        super(context, adapter, R.string.santa_snap, R.drawable.android_game_cards_santa_snap);
    }

    @Override
    public int getFeatureModuleNameId() {
        return R.string.feature_santa_snap;
    }

    @Override
    public void onClick(View v) {
        switch (getState()) {
            case STATE_READY:
            case STATE_FINISHED:
                Intent intent =
                        SplashActivity.getIntent(
                                mContext.getActivity(),
                                getCardDrawableRes(),
                                R.string.santa_snap,
                                getFeatureModuleNameId(),
                                R.color.santa_snap_splash_screen_background,
                                false /* portrait */,
                                getTitle(),
                                getImageView(),
                                mContext.getApplicationContext().getPackageName(),
                                "com.google.android.apps.santatracker.santasnap.SantaSnapActivity");
                mContext.launchActivity(intent);
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), getDisabledString(R.string.santa_snap));
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), R.string.santa_snap_locked);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (getState()) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getApplicationContext(), R.string.santa_snap);
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), getDisabledString(R.string.santa_snap));
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), R.string.santa_snap_locked);
                break;
        }
        return true;
    }

    @Override
    public boolean handleVoiceAction(Intent intent) {
        return clickIfMatchesDescription(
                intent, VoiceAction.ACTION_PLAY_GAME, VoiceAction.ACTION_PLAY_GAME_EXTRA);
    }
}
