/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.google.android.libraries.cast.companionlibrary.remotecontrol;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions
        .TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.notification.VideoCastNotificationService;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * A {@link BroadcastReceiver} for receiving media button actions (from the lock screen) as well as
 * the the status bar notification media actions.
 */
public class VideoIntentReceiver extends BroadcastReceiver {

    private static final String TAG = LogUtils.makeLogTag(VideoIntentReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        VideoCastManager castMgr = VideoCastManager.getInstance();
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case VideoCastNotificationService.ACTION_TOGGLE_PLAYBACK:
                try {
                    VideoCastManager.getInstance().togglePlayback();
                } catch (CastException | TransientNetworkDisconnectionException |
                        NoConnectionException e) {
                    LOGE(TAG, "onReceive() Failed to toggle playback ");
                }
                break;
            case VideoCastNotificationService.ACTION_STOP:
                LOGD(TAG, "Calling stopApplication from intent");
                castMgr.disconnect();
                break;
            case Intent.ACTION_MEDIA_BUTTON:
                // this is used when we toggle playback from lockscreen in versions prior to
                // Lollipop
                if (!intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
                    return;
                }
                KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                    return;
                }

                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    try {
                        VideoCastManager.getInstance().togglePlayback();
                    } catch (CastException | TransientNetworkDisconnectionException |
                            NoConnectionException e) {
                        LOGE(TAG, "onReceive() Failed to toggle playback ");
                    }
                }
                break;
        }
    }
}
