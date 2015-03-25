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
import com.google.android.apps.santatracker.launch.StartupActivity;
import com.google.android.apps.santatracker.util.LruImageCache;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;

/**
 * Helper methods to create notifications.
 */
public abstract class SantaNotificationBuilder {

    // private static final String TAG = "SantaNotificationBuilder";

    /**
     * Construct a generic Santa notification with a headline title.
     */
    private static Notification GetNotification(Context c, int headline) {
        Resources r = c.getResources();

        // Add the wearable extender with a different notification background
        Bitmap largeIcon = BitmapFactory.decodeResource(r,
                R.drawable.santa_notification_background);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(false)
                        .setBackground(largeIcon);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c)
                .setSmallIcon(R.drawable.notification_small)
                .setColor(c.getResources().getColor(R.color.brandSantaTracker))
                .setAutoCancel(true)
                .setContentTitle(r.getString(headline))
                .setContentText(r.getString(R.string.track_santa))
                .extend(wearableExtender);

        // Add the type of notification for wearable to app tracking of clicks
        Intent i = new Intent(c, StartupActivity.class);
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, NotificationConstants.TAKEOFF_PATH);

        // Add the intent to open the main startup activity when clicked
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addParentStack(StartupActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder.build();
    }

    /**
     * Display a generic Santa notification with content loaded from a string resource.
     */
    public static void CreateSantaNotification(Context c, int content) {
        Notification n = GetNotification(c, content);

        //Post the notification.
        NotificationManagerCompat.from(c)
                .notify(NotificationConstants.NOTIFICATION_ID, n);
    }


    /**
     * Display an info notification with an optional photo loaded from a URL.
     */
    public static void CreateInfoNotification(final Context c, final String title,
            final String text,
            final String photoUrl) {

        if (photoUrl != null) {
            // Load the photo through the Volley library
            RequestQueue requestQueue = Volley.newRequestQueue(c);
            requestQueue.start();
            ImageLoader imageLoader = new ImageLoader(requestQueue, new LruImageCache());
            // Wait until the photo has been loaded to display the notification
            imageLoader.get(photoUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    CreateInfoNotificationWithBitmap(c, title, text, response.getBitmap());
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    CreateInfoNotificationWithBitmap(c, title, text, null);
                }
            });
        } else {
            CreateInfoNotificationWithBitmap(c, title, text, null);
        }
    }

    /**
     * Display a notification with a bitmap as the background for Wear.
     * If no bitmap is set, the default Santa notification background is shown instead.
     */
    private static void CreateInfoNotificationWithBitmap(Context c, String title, String text,
            Bitmap photo) {

        Resources r = c.getResources();
        Bitmap largeIcon = BitmapFactory.decodeResource(r,
                R.drawable.santa_info_notification_background);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(false)
                        .setBackground(largeIcon);

        if (photo != null) {
            // Set the bitmap as a background for Wear
            NotificationCompat.Builder page = new NotificationCompat.Builder(c)
                    .setSmallIcon(R.drawable.notification_small)
                    .setContentText(text)
                    .extend(new NotificationCompat.WearableExtender().setBackground(photo));
            wearableExtender.addPage(page.build());
        }

        // Make main notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c)
                .setSmallIcon(R.drawable.notification_small)
                .setColor(c.getResources().getColor(R.color.brandSantaTrackerDark))
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .setContentTitle(title)
                .extend(wearableExtender);

        if (photo == null) {
            mBuilder.setContentText(text);
        }

        Intent i = new Intent(c, StartupActivity.class);
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, NotificationConstants.KEY_LOCATION);

        // Add Intent to open main startup activity (or return to it if the app is already open.)
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addParentStack(StartupActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        Notification n = mBuilder.build();

        // Fire off the notification
        NotificationManager mNotificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NotificationConstants.NOTIFICATION_ID, n);
    }

    /**
     * Dismiss all notifications.
     */
    public static void DismissNotifications(Context c) {
        NotificationManager mNotificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    /**
     * Schedule a basic notification at an approximate time.
     */
    public static void ScheduleSantaNotification(Context c, long timestamp, int notificationType) {

        // Only schedule a notification if the time is in the future
        if (timestamp < System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarm = (AlarmManager) c
                .getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(c, NotificationBroadcastReceiver.class);
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_ID,
                NotificationConstants.NOTIFICATION_ID);

        // Type is "takeoff", "location", etc.
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, notificationType);

        // Generate unique pending intent
        PendingIntent pi = PendingIntent.getBroadcast(c, notificationType, i, 0);

        // Deliver next time the device is woken up
        alarm.set(AlarmManager.RTC, timestamp, pi);

    }

}
