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

package com.google.android.apps.santatracker.util;

import com.google.android.apps.santatracker.NotificationBroadcastReceiver;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.data.DestinationDbHelper;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.data.StreamCursor;
import com.google.android.apps.santatracker.data.StreamDbHelper;
import com.google.android.apps.santatracker.data.StreamEntry;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

/**
 * Intentservice that schedules the next (wear) notification. Notifications are scheduled as
 * PendingIntents that are received by the {@link com.google.android.apps.santatracker.NotificationBroadcastReceiver}.
 * Scheduling these notifications requires database access, which is why the logic has been moved
 * here.
 */
public class ScheduleNotificationService extends IntentService {

    private static final String TAG = "ScheduleNotificationService";

    public ScheduleNotificationService() {
        super("ScheduleNotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        scheduleNextNotification();
    }

    private void scheduleNextNotification() {
        SantaLog.d(TAG, "Scheduling next Notification");
        StreamDbHelper db = StreamDbHelper.getInstance(this);

        // Get all following notifications
        Cursor c = db.getFollowing(SantaPreferences.getCurrentTime(), true);
        if (c.isAfterLast()) {
            // No notifications left, cancel existing notification and do not schedule a new one
            cancelPending();
            return;
        }
        StreamCursor stream = new StreamCursor(c);
        StreamEntry entry = stream.getCurrent();
        c.close();

        DestinationDbHelper destionationHelper = DestinationDbHelper.getInstance(this);
        final long finalArrival = destionationHelper.getLastArrival();

        // Schedule execution
        Intent i = new Intent(getApplicationContext(), NotificationBroadcastReceiver.class);
        i.putExtra(NotificationConstants.KEY_TIMESTAMP, entry.timestamp);
        i.putExtra(NotificationConstants.KEY_FACT, entry.didYouKnow);
        i.putExtra(NotificationConstants.KEY_IMAGEURL, entry.image);
        i.putExtra(NotificationConstants.KEY_STATUS, entry.santaStatus);
        i.putExtra(NotificationConstants.KEY_FINAL_ARRIVAL, finalArrival);

        // Notification type
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE,
                NotificationConstants.NOTIFICATION_FACT);

        // Overwrite any already pending intents
        PendingIntent pi = getPendingIntent(i);

        final long time = SantaPreferences.getAdjustedTime(entry.timestamp);

        // Only schedule a notification if the time is in the future, otherwise skip it
        if(time < System.currentTimeMillis()){
            return ;
        }

        // Deliver next time the device is woken up
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC, time, pi);

        SantaLog.d(TAG, "Scheduled notification: " + time + " ; in: " + (System.currentTimeMillis()
                - time));
    }

    private PendingIntent getPendingIntent(Intent i) {
        return PendingIntent
                .getBroadcast(getApplicationContext(), NotificationConstants.NOTIFICATION_FACT, i,
                        PendingIntent.FLAG_CANCEL_CURRENT);

    }

    private void cancelPending() {
        SantaLog.d(TAG, "Cancelled pending intent.");
        // Need identical intent (sans extras) to cancel pending intent.
        getPendingIntent(new Intent()).cancel();
    }
}
