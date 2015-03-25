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
import com.google.android.apps.santatracker.games.MemoryActivity;

import android.view.View;

/**
 * Launch the Memory Match game.
 */
public class LaunchMemory extends AbstractLaunch {

    public LaunchMemory(StartupActivity.SantaContext context) {
        super(context, R.string.memory, R.drawable.marker_badge_memory, R.color.SantaGreen,
                R.dimen.markerMemoryPaddingSides, R.dimen.markerMemoryPaddingTop,
                R.dimen.markerMemoryPaddingSides);
    }

    static public int getId() {
        return R.string.memory;
    }

    @Override
    public void onClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                mContext.launchActivity(MemoryActivity.class);
                break;
            case STATE_DISABLED:
                notify(mContext.getContext(), R.string.memory_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getContext(), R.string.memory_locked);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getContext(), R.string.memory);
                break;
            case STATE_DISABLED:
                notify(mContext.getContext(), R.string.memory_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getContext(), R.string.memory_locked);
                break;
        }
        return true;
    }

}
