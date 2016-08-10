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

package com.google.android.apps.santatracker;

import com.google.android.apps.santatracker.common.NotificationConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int type = intent.getIntExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, -1);

        switch (type) {
            case NotificationConstants.NOTIFICATION_NEARBY:
                SantaNotificationBuilder.CreateSantaNotification(context,
                        R.string.notification_nearby);
                //requestWearNotification(context, R.string.notification_nearby);
                break;
            case NotificationConstants.NOTIFICATION_TAKEOFF:
                SantaNotificationBuilder
                        .CreateSantaNotification(context, R.string.notification_takeoff);
                //requestWearNotification(context, R.string.notification_takeoff);
                break;
            case NotificationConstants.NOTIFICATION_LOCATION:
                String fact = intent.getStringExtra(NotificationConstants.KEY_LOCATION_FACT);
                String location = intent.getStringExtra(NotificationConstants.KEY_LOCATION);
                String photoUrl = intent.getStringExtra(NotificationConstants.KEY_LOCATION_PHOTO);
                String mapUrl = intent.getStringExtra(NotificationConstants.KEY_LOCATION_MAP);
                SantaNotificationBuilder
                        .CreateTriviaNotification(context, location, photoUrl, mapUrl, fact);
                break;
            case NotificationConstants.NOTIFICATION_FACT:
                processFactNotification(context, intent);
                break;
        }
    }

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

    private void requestWearNotification(Context context, int content) {
        Intent wearIntent = new Intent(context, PhoneNotificationService.class);
        wearIntent.setAction(NotificationConstants.ACTION_SEND);
        wearIntent.putExtra(NotificationConstants.KEY_CONTENT,
                context.getResources().getString(content));
        context.startService(wearIntent);
    }

}
