/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.jetpack.JetpackActivity;

import android.view.View;

/**
 * Launch the Elf Jetpack game.
 */
public class LaunchJetpack extends AbstractLaunch {

    public LaunchJetpack(StartupActivity.SantaContext context) {
        super(context, R.string.elf_jetpack, R.drawable.marker_badge_jetpack, R.color.SantaOrange,
                R.dimen.markerJetpackPaddingSides, R.dimen.markerJetpackPaddingTop,
                R.dimen.markerJetpackPaddingSides);
    }

    static public int getId() {
        return R.string.elf_jetpack;
    }

    @Override
    public void onClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                mContext.launchActivity(JetpackActivity.class);
                break;
            case STATE_DISABLED:
                notify(mContext.getContext(), R.string.elf_jetpack_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getContext(), R.string.elf_jetpack_locked);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getContext(), R.string.elf_jetpack);
                break;
            case STATE_DISABLED:
                notify(mContext.getContext(), R.string.elf_jetpack_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getContext(), R.string.elf_jetpack_locked);
                break;
        }
        return true;
    }

}
