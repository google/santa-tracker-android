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

package com.google.android.apps.santatracker;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.launch.StartupActivity;

public class SantaNotificationBuilder {

    private static Notification getNotification(Context c, int headline) {
        Resources r = c.getResources();
        Bitmap largeIcon =
                BitmapFactory.decodeResource(r, R.drawable.santa_notification_background);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(false)
                        .setBackground(largeIcon);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(c, NotificationConstants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification_small)
                        .setColor(ContextCompat.getColor(c, R.color.brandSantaTracker))
                        .setAutoCancel(true)
                        .setContentTitle(r.getString(headline))
                        .setContentText(r.getString(R.string.track_santa))
                        .extend(wearableExtender);

        Intent i = new Intent(c, StartupActivity.class);
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, NotificationConstants.TAKEOFF_PATH);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addParentStack(StartupActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();
    }

    public static void createSantaNotification(Context c, int content) {
        Notification n = getNotification(c, content);

        // Post the notification.
        NotificationManagerCompat.from(c).notify(NotificationConstants.NOTIFICATION_ID, n);
    }

    /** Dismiss all notifications. */
    public static void dismissNotifications(Context c) {
        NotificationManager mNotificationManager =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name_santa);

            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(NotificationConstants.CHANNEL_ID, name, importance);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager nmgr = context.getSystemService(NotificationManager.class);
            nmgr.createNotificationChannel(channel);
        }
    }

    public static void scheduleSantaNotification(Context c, long timestamp, int notificationType) {
        // Only schedule a notification if the time is in the future
        if (timestamp < System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarm = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(c, NotificationBroadcastReceiver.class);
        i.putExtra(
                NotificationConstants.KEY_NOTIFICATION_ID, NotificationConstants.NOTIFICATION_ID);

        // Type is "takeoff", "location", etc.
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, notificationType);

        // Generate unique pending intent
        PendingIntent pi = PendingIntent.getBroadcast(c, notificationType, i, 0);

        // Deliver next time the device is woken up
        alarm.set(AlarmManager.RTC, timestamp, pi);
    }
}
