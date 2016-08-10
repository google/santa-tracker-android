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

package com.google.android.apps.santatracker.cast;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.launch.StartupActivity;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

/**
 * A service to provide status bar Notifications when we are casting. For JB+ versions,
 * notification
 * area provides a play/pause toggle and an "x" button to disconnect but that for GB, we do not
 * show that due to the framework limitations.
 */
public class DataCastNotificationService extends Service {

    private static final String TAG = LogUtils.makeLogTag(DataCastNotificationService.class);


    public static final String ACTION_STOP =
            "com.google.android.apps.santatracker.cast.action.stop";

    public static final String ACTION_VISIBILITY =
            "com.google.android.apps.santatracker.cast.action.notificationvisibility";

    private static final int NOTIFICATION_ID = 1;

    public static final String NOTIFICATION_VISIBILITY = "visible";

    private static Class<?> INTENT_ACTIVITY = StartupActivity.class;

    private Notification mNotification;

    private boolean mVisible;

    private DataCastManager mCastManager;

    private DataCastConsumerImpl mConsumer;


    @Override
    public void onCreate() {
        super.onCreate();

        mCastManager = NotificationDataCastManager.getInstance();
        if (!mCastManager.isConnected() && !mCastManager.isConnecting()) {
            mCastManager.reconnectSessionIfPossible();
        }
        mConsumer = new DataCastConsumerImpl() {
            @Override
            public void onApplicationDisconnected(int errorCode) {
                LOGD(TAG, "onApplicationDisconnected() was reached, stopping the notification"
                        + " service");
                stopSelf();
            }

            @Override
            public void onUiVisibilityChanged(boolean visible) {
                mVisible = !visible;
                if (mVisible && (mNotification != null)) {
                    startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    stopForeground(true);
                }
            }
        };
        mCastManager.addDataCastConsumer(mConsumer);

    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGD(TAG, "onStartCommand");
        if (intent != null) {

            String action = intent.getAction();
            if (ACTION_VISIBILITY.equals(action)) {
                mVisible = intent.getBooleanExtra(NOTIFICATION_VISIBILITY, false);
                LOGD(TAG, "onStartCommand(): Action: ACTION_VISIBILITY " + mVisible);
                if (mNotification == null) {
                    setUpNotification();
                }
                if (mVisible && mNotification != null) {
                    startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    stopForeground(true);
                }
            } else {
                LOGD(TAG, "onStartCommand(): Action: none");
            }

        } else {
            LOGD(TAG, "onStartCommand(): Intent was null");
        }

        return Service.START_STICKY;
    }

    private void setUpNotification() {
        build(R.string.app_name_santa, R.drawable.ic_launcher_santa);
    }

    /**
     * Removes the existing notification.
     */
    private void removeNotification() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).
                cancel(NOTIFICATION_ID);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        removeNotification();
        if (mCastManager != null && mConsumer != null) {
            mCastManager.removeDataCastConsumer(mConsumer);
            mCastManager = null;
        }
    }

    /*
     * Build the RemoteViews for the notification. We also need to add the appropriate "back stack"
     * so when user goes into the CastPlayerActivity, she can have a meaningful "back" experience.
     */
    private void build(int titleId, int imageId) {

        // Main Content PendingIntent
        Intent contentIntent = new Intent(this, INTENT_ACTIVITY);

        // Disconnect PendingIntent
        Intent stopIntent = new Intent(ACTION_STOP);
        stopIntent.setPackage(getPackageName());
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        // Media metadata
        String castingTo = getResources().getString(R.string.ccl_casting_to_device,
                mCastManager.getDeviceName());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(INTENT_ACTIVITY);
        stackBuilder.addNextIntent(contentIntent);
        PendingIntent contentPendingIntent =
                stackBuilder.getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder
                = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_small)
                .setContentTitle(getResources().getString(titleId))
                .setContentText(castingTo)
                .setContentIntent(contentPendingIntent)
                .setColor(ContextCompat.getColor(this, R.color.brandSantaTracker))
                .addAction(R.drawable.ic_notification_disconnect_24dp,
                        getString(R.string.ccl_disconnect),
                        stopPendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        mNotification = builder.build();

    }

    /*
     * We try to disconnect application but even if that fails, we need to remove notification since
     * that is the only way to get rid of it without going to the application
     */
    private void stopApplication() {
        try {
            LOGD(TAG, "Calling stopApplication");
            mCastManager.disconnect();
        } catch (Exception e) {
            LOGE(TAG, "Failed to disconnect application", e);
        }
        stopSelf();
    }

}
