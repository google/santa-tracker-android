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

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.Presenter;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.tracker.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TvCardAdapter extends ArrayObjectAdapter implements LauncherDataChangedCallback {

    public static final int SANTA = 0;
    public static final int VIDEO01 = 1;
    public static final int VIDEO15 = 2;
    public static final int ROCKET = 3;
    public static final int VIDEO23 = 4;

    public static final int NUM_PINS = 5;

    private AbstractLaunch[] mAllLaunchers = new AbstractLaunch[NUM_PINS];
    private AbstractLaunch[] mLaunchers;

    public TvCardAdapter(SantaContext santaContext, Presenter presenter, Clock clock) {
        super(presenter);

        mAllLaunchers[SANTA] = new LaunchSantaTracker(santaContext, this);
        mAllLaunchers[VIDEO01] =
                new LaunchVideo(
                        santaContext,
                        this,
                        R.string.video_santa_is_back,
                        R.drawable.android_video_cards_santas_back,
                        1,
                        clock);
        mAllLaunchers[VIDEO15] =
                new LaunchVideo(
                        santaContext,
                        this,
                        R.string.video_office_prank,
                        R.drawable.android_video_cards_office_prank,
                        1,
                        clock);
        mAllLaunchers[ROCKET] = new LaunchRocketSleigh(santaContext, this);
        mAllLaunchers[VIDEO23] =
                new LaunchVideo(
                        santaContext,
                        this,
                        R.string.video_carpool,
                        R.drawable.android_video_cards_carpool,
                        23,
                        clock);

        setHasStableIds(false);
        refreshData();
    }

    public void updateMarkerVisibility() {
        List<AbstractLaunch> launchers = new ArrayList<>(NUM_PINS);
        for (AbstractLaunch l : mAllLaunchers) {
            if (l.getState() != AbstractLaunch.STATE_HIDDEN) {
                launchers.add(l);
            }
        }
        mLaunchers = launchers.toArray(new AbstractLaunch[launchers.size()]);
    }

    public AbstractLaunch[] getLaunchers() {
        return mAllLaunchers;
    }

    public AbstractLaunch getLauncher(int i) {
        return mAllLaunchers[i];
    }

    @Override
    public void refreshData() {
        updateMarkerVisibility();
        clear();
        addAll(0, Arrays.asList(mLaunchers));
    }
}
