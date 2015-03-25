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
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.actions.SearchIntents;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Launch the Santa Tracker screen.
 * This is only a place holder implementation that only displays a message when the tracker
 * should be launched.
 */
public class LaunchSanta extends AbstractLaunch {

    protected static final String TAG = "LaunchSanta";

    private SimpleCommandExecutor mExecutor = new SimpleCommandExecutor();

    public LaunchSanta(StartupActivity.SantaContext context) {
        super(context, R.string.track_santa, R.drawable.marker_badge_santa, R.color.SantaGreen,
                R.dimen.markerBadgeDefaultPadding, R.dimen.markerBadgeDefaultPadding,
                R.dimen.markerBadgeDefaultPadding);
    }

    static public int getId() {
        return R.string.track_santa;
    }

    @Override
    public void onClick(View v) {
        mExecutor.cancelAll(); // touchscreen action cancels all pending voice commands
        switch (mState) {
            case STATE_READY:
                // Launch Santa Tracker
                Toast.makeText(mContext.getContext(), "Tracker would be launched here.",
                        Toast.LENGTH_SHORT).show();
                break;
            case STATE_LOCKED:
                // Tracker is still locked, waiting for 24th December
                notify(mContext.getContext(), R.string.santa_locked);
                break;
            case STATE_FINISHED:
                // Tracking is over, waiting for next year
                notify(mContext.getContext(), R.string.santa_is_busy_preparing_for_next_year);
                break;
            case STATE_DISABLED:
            default:
                // Tracker is unavailable (insufficient data to launch tracker)
                notify(mContext.getContext(), R.string.still_trying_to_reach_santa);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mState) {
            case STATE_READY:
                notify(mContext.getContext(), R.string.track_santa);
                break;
            case STATE_LOCKED:
                notify(mContext.getContext(), R.string.santa_locked);
                break;
            case STATE_FINISHED:
                notify(mContext.getContext(), R.string.santa_is_busy_preparing_for_next_year);
                break;
            case STATE_DISABLED:
            default:
                notify(mContext.getContext(), R.string.still_trying_to_reach_santa);
                break;
        }
        return true;
    }

    @Override
    public boolean handleVoiceAction(Intent intent) {
        String action = intent.getAction();
        if (SearchIntents.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, String.format("Voice command: search for [%s] on Santa Tracker", query));
            if (isSupportedQuery(query)) {
                handleVoiceSearchForSanta();
                // if we got here, the voice command syntax was OK
                // so even though we may not actually pop the Santa map view, we'll
                // report back that we handled (or at least tried to) the voice command
                return true;
            }
        }
        return false;
    }


    private void handleVoiceSearchForSanta() {
        switch (mState) {
            case STATE_READY: // highly unlikely to be ready upon the first invocation
                SantaLog.d(TAG, "Got lucky. Launching SantaMapActivity.");
                Toast.makeText(mContext.getContext(), "Hello, Santa!", Toast.LENGTH_SHORT).show();
                break;
            case STATE_FINISHED: // FINISHED -> READY transition does happen with local API
            case STATE_DISABLED:
            case STATE_LOCKED:
            default:
                notify(mContext.getContext(), R.string.contacting_santa);
                scheduleVoiceCommand();
                break;
        }
    }

    private void scheduleVoiceCommand() {
        Log.d(TAG, "Not ready. Scheduling for later.");
        Runnable launchSantaMap = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Launching SantaMapActivity.");
                Toast.makeText(mContext.getContext(), "Hello, Santa!", Toast.LENGTH_SHORT).show();
            }
        };
        mExecutor.execute(launchSantaMap);
    }

    private boolean isSupportedQuery(String query) {
        Resources res = mContext.getResources();
        String[] supportedQueries = res.getStringArray(R.array.voice_command_search_for);
        return Arrays.asList(supportedQueries).contains(query.toLowerCase());
    }


    @Override
    public void setState(int state) {
        super.setState(state);
        SantaLog.v(TAG, String.format("setState to [%d]", state));
        // if the transition was into READY state, execute all pending commands
        if (mState == STATE_READY) {
            mExecutor.executeAll();
        }
    }

    /**
     * Queues up commands until explicit executeAll invocation.
     * Currently max queue length is 1.
     */
    static class SimpleCommandExecutor implements Executor {

        public static final int TIMEOUT_MS = 30 * 1000; // give up after 30 seconds

        private Runnable mPendingCommand;

        private long mTimeStamp;

        @Override
        public void execute(Runnable command) {
            synchronized (this) {
                cancelAll();
                mPendingCommand = command;
                mTimeStamp = System.currentTimeMillis();
            }
        }

        public synchronized void cancelAll() {
            mPendingCommand = null;
            mTimeStamp = 0L;
        }

        public synchronized void executeAll() {
            if (mPendingCommand != null) {
                if (System.currentTimeMillis() - mTimeStamp <= TIMEOUT_MS) {
                    mPendingCommand.run();
                } else {
                    Log.d(TAG, "Pending command timed out, ignoring.");
                }
                mPendingCommand = null;
                mTimeStamp = 0L;
            }
        }
    }

    @Override
    public boolean isGame() {
        return false;
    }
}

