/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.launch.StartupActivity;

public class SantaNotificationBuilder {

    // private static final String TAG = "SantaNotificationBuilder";

    private static Notification getNotification(Context c, int headline) {
        Resources r = c.getResources();
        Bitmap largeIcon = BitmapFactory.decodeResource(r,
                R.drawable.santa_notification_background);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(false)
                        .setBackground(largeIcon);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c)
                .setSmallIcon(R.drawable.notification_small)
                .setColor(ResourcesCompat.getColor(c.getResources(),
                        R.color.brandSantaTracker, c.getTheme()))
                .setAutoCancel(true)
                .setContentTitle(r.getString(headline))
                .setContentText(r.getString(R.string.track_santa))
                .extend(wearableExtender);

        Intent i = new Intent(c, StartupActivity.class);
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, NotificationConstants.TAKEOFF_PATH);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addParentStack(StartupActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder.build();
    }

    public static void createSantaNotification(Context c, int content) {
        Notification n = getNotification(c, content);

        //Post the notification.
        NotificationManagerCompat.from(c)
                .notify(NotificationConstants.NOTIFICATION_ID, n);
    }

    public static void createInfoNotification(final Context c, final String title,
            final String text, final String photoUrl) {

        if (photoUrl != null) {
            Glide.with(c).load(photoUrl).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource,
                                            GlideAnimation<? super Bitmap> glideAnimation) {
                    createInfoNotificationWithBitmap(c, title, text, resource);
                }

                @Override
                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    createInfoNotificationWithBitmap(c, title, text, null);
                }
            });
        } else {
            createInfoNotificationWithBitmap(c, title, text, null);
        }
    }

    private static void createInfoNotificationWithBitmap(Context c, String title, String text,
            Bitmap photo) {

        Resources r = c.getResources();
        Bitmap largeIcon = BitmapFactory.decodeResource(r,
                R.drawable.santa_info_notification_background);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(false)
                        .setBackground(largeIcon);

        if (photo != null) {
            NotificationCompat.Builder page = new NotificationCompat.Builder(c)
                    .setSmallIcon(R.drawable.notification_small)
                    .setContentText(text)
                    .extend(new NotificationCompat.WearableExtender().setBackground(photo));
            wearableExtender.addPage(page.build());
        }

        // Make main notification

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c)
                .setSmallIcon(R.drawable.notification_small)
                .setColor(ResourcesCompat.getColor(c.getResources(),
                        R.color.brandSantaTrackerDark, c.getTheme()))
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .setContentTitle(title)
                .extend(wearableExtender);

        if (photo == null) {
            mBuilder.setContentText(text);
        }

        Intent i = new Intent(c, StartupActivity.class);
        i.putExtra(NotificationConstants.KEY_NOTIFICATION_TYPE, NotificationConstants.KEY_LOCATION);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addParentStack(StartupActivity.class);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        Notification n = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NotificationConstants.NOTIFICATION_ID, n);
    }

    /**
     * Dismiss all notifications.
     */
    public static void dismissNotifications(Context c) {
        NotificationManager mNotificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        // Send a broadcast to the phone's service, so it will update the data API to
        // let the watch know to cancel all its corresponding notifications.
        Intent dismissWearableNotifications = new Intent(c, PhoneNotificationService.class);
        dismissWearableNotifications.setAction(NotificationConstants.ACTION_DISMISS);
        c.sendBroadcast(dismissWearableNotifications);

    }

    public static void scheduleSantaNotification(Context c, long timestamp, int notificationType) {

        // Only schedule a notification if the time is in the future
        if(timestamp < System.currentTimeMillis()){
            return ;
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
