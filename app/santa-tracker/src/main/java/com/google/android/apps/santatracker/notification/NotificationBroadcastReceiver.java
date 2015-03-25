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

package com.google.android.apps.santatracker.notification;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.common.NotificationConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver that displays a notification.
 * Notifications are defined from constants in
 * {@link com.google.android.apps.santatracker.common.NotificationConstants}.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationBroadcastReceiver";

    /**
     * Display the notification encoded within the {@link Intent}.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        int type = intent.getIntExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, -1);

        switch (type) {
            case NotificationConstants.NOTIFICATION_TAKEOFF:
                SantaNotificationBuilder
                        .CreateSantaNotification(context, R.string.notification_takeoff);
                break;
            case NotificationConstants.NOTIFICATION_INFO:
                processFactNotification(context, intent);
                break;
        }
    }

    /**
     * Displays a 'fact' notification (fact or status with an optional image) encoded within the
     * Intent.
     *
     * @see com.google.android.apps.santatracker.common.NotificationConstants
     */
    private void processFactNotification(Context context, Intent intent) {
        final long finalArrival = intent.getLongExtra(NotificationConstants.KEY_FINAL_ARRIVAL, 0);
        final long timestamp = intent.getLongExtra(NotificationConstants.KEY_TIMESTAMP, 0);

        // Sanity check to make sure Santa is still travelling
        if (timestamp > finalArrival) {
            return;
        }

        final String didyouknow = intent.getStringExtra(NotificationConstants.KEY_FACT);
        final String imageUrl = intent.getStringExtra(NotificationConstants.KEY_IMAGEURL);
        final String status = intent.getStringExtra(NotificationConstants.KEY_STATUS);

        String title;
        String text;
        if (didyouknow != null) {
            title = context.getString(R.string.did_you_know);
            text = didyouknow;
        } else {
            title = context.getString(R.string.update_from_santa);
            text = status;
        }
        // Schedule the next notification
        SantaNotificationBuilder.CreateInfoNotification(context, title, text, imageUrl);
    }

}
