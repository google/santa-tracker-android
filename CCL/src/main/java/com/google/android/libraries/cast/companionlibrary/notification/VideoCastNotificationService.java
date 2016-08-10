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

package com.google.android.libraries.cast.companionlibrary.notification;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerActivity;
import com.google.android.libraries.cast.companionlibrary.utils.FetchBitmapTask;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

/**
 * A service to provide status bar Notifications when we are casting. For JB+ versions, notification
 * area provides a play/pause toggle and an "x" button to disconnect but that for GB, we do not
 * show that due to the framework limitations.
 */
public class VideoCastNotificationService extends Service {

    private static final String TAG = LogUtils.makeLogTag(VideoCastNotificationService.class);

    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.google.android.libraries.cast.companionlibrary.action.toggleplayback";
    public static final String ACTION_STOP =
            "com.google.android.libraries.cast.companionlibrary.action.stop";
    public static final String ACTION_VISIBILITY =
            "com.google.android.libraries.cast.companionlibrary.action.notificationvisibility";
    private static final int NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_VISIBILITY = "visible";

    private Bitmap mVideoArtBitmap;
    private boolean mIsPlaying;
    private Class<?> mTargetActivity;
    private int mOldStatus = -1;
    private Notification mNotification;
    private boolean mVisible;
    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mConsumer;
    private FetchBitmapTask mBitmapDecoderTask;
    private int mDimensionInPixels;

    @Override
    public void onCreate() {
        super.onCreate();
        mDimensionInPixels = Utils.convertDpToPixel(VideoCastNotificationService.this,
                getResources().getDimension(R.dimen.ccl_notification_image_size));
        mCastManager = VideoCastManager.getInstance();
        readPersistedData();
        if (!mCastManager.isConnected() && !mCastManager.isConnecting()) {
            mCastManager.reconnectSessionIfPossible();
        }
        mConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onApplicationDisconnected(int errorCode) {
                LOGD(TAG, "onApplicationDisconnected() was reached, stopping the notification"
                        + " service");
                stopSelf();
            }

            @Override
            public void onRemoteMediaPlayerStatusUpdated() {
                int mediaStatus = mCastManager.getPlaybackStatus();
                VideoCastNotificationService.this.onRemoteMediaPlayerStatusUpdated(mediaStatus);
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
        mCastManager.addVideoCastConsumer(mConsumer);

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
                onRemoteMediaPlayerStatusUpdated(mCastManager.getPlaybackStatus());
                if (mNotification == null) {
                    try {
                        setUpNotification(mCastManager.getRemoteMediaInformation());
                    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                        LOGE(TAG, "onStartCommand() failed to get media", e);
                    }
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

    private void setUpNotification(final MediaInfo info)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        if (info == null) {
            return;
        }
        if (mBitmapDecoderTask != null) {
            mBitmapDecoderTask.cancel(false);
        }
        Uri imgUri = null;
        try {
            if (!info.getMetadata().hasImages()) {
                build(info, null, mIsPlaying);
                return;
            } else {
                imgUri = info.getMetadata().getImages().get(0).getUrl();
            }
        } catch (CastException e) {
            LOGE(TAG, "Failed to build notification", e);
        }
        mBitmapDecoderTask = new FetchBitmapTask() {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                try {
                    mVideoArtBitmap = Utils.scaleAndCenterCropBitmap(bitmap, mDimensionInPixels,
                            mDimensionInPixels);
                    build(info, mVideoArtBitmap, mIsPlaying);
                } catch (CastException | NoConnectionException
                        | TransientNetworkDisconnectionException e) {
                    LOGE(TAG, "Failed to set notification for " + info.toString(), e);
                }
                if (mVisible && (mNotification != null)) {
                    startForeground(NOTIFICATION_ID, mNotification);
                }
                if (this == mBitmapDecoderTask) {
                    mBitmapDecoderTask = null;
                }
            }
        };
        mBitmapDecoderTask.execute(imgUri);
    }

    /**
     * Removes the existing notification.
     */
    private void removeNotification() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).
                cancel(NOTIFICATION_ID);
    }

    private void onRemoteMediaPlayerStatusUpdated(int mediaStatus) {
        if (mOldStatus == mediaStatus) {
            // not need to make any updates here
            return;
        }
        mOldStatus = mediaStatus;
        LOGD(TAG, "onRemoteMediaPlayerStatusUpdated() reached with status: " + mediaStatus);
        try {
            switch (mediaStatus) {
                case MediaStatus.PLAYER_STATE_BUFFERING: // (== 4)
                    mIsPlaying = false;
                    setUpNotification(mCastManager.getRemoteMediaInformation());
                    break;
                case MediaStatus.PLAYER_STATE_PLAYING: // (== 2)
                    mIsPlaying = true;
                    setUpNotification(mCastManager.getRemoteMediaInformation());
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED: // (== 3)
                    mIsPlaying = false;
                    setUpNotification(mCastManager.getRemoteMediaInformation());
                    break;
                case MediaStatus.PLAYER_STATE_IDLE: // (== 1)
                    mIsPlaying = false;
                    if (!mCastManager.shouldRemoteUiBeVisible(mediaStatus,
                            mCastManager.getIdleReason())) {
                        stopForeground(true);
                    } else {
                        setUpNotification(mCastManager.getRemoteMediaInformation());
                    }
                    break;
                case MediaStatus.PLAYER_STATE_UNKNOWN: // (== 0)
                    mIsPlaying = false;
                    stopForeground(true);
                    break;
                default:
                    break;
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            LOGE(TAG, "Failed to update the playback status due to network issues", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (mBitmapDecoderTask != null) {
            mBitmapDecoderTask.cancel(false);
        }
        removeNotification();
        if (mCastManager != null && mConsumer != null) {
            mCastManager.removeVideoCastConsumer(mConsumer);
            mCastManager = null;
        }
    }

    /*
     * Build the RemoteViews for the notification. We also need to add the appropriate "back stack"
     * so when user goes into the CastPlayerActivity, she can have a meaningful "back" experience.
     */
    private void build(MediaInfo info, Bitmap bitmap, boolean isPlaying)
            throws CastException, TransientNetworkDisconnectionException, NoConnectionException {

        // Playback PendingIntent
        Intent playbackIntent = new Intent(ACTION_TOGGLE_PLAYBACK);
        playbackIntent.setPackage(getPackageName());
        PendingIntent playbackPendingIntent = PendingIntent
                .getBroadcast(this, 0, playbackIntent, 0);

        // Disconnect PendingIntent
        Intent stopIntent = new Intent(ACTION_STOP);
        stopIntent.setPackage(getPackageName());
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        // Main Content PendingIntent
        Bundle mediaWrapper = Utils.mediaInfoToBundle(mCastManager.getRemoteMediaInformation());
        Intent contentIntent = new Intent(this, mTargetActivity);
        contentIntent.putExtra(VideoCastManager.EXTRA_MEDIA, mediaWrapper);

        // Media metadata
        MediaMetadata metadata = info.getMetadata();
        String castingTo = getResources().getString(R.string.ccl_casting_to_device,
                mCastManager.getDeviceName());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(mTargetActivity);
        stackBuilder.addNextIntent(contentIntent);
        if (stackBuilder.getIntentCount() > 1) {
            stackBuilder.editIntentAt(1).putExtra(VideoCastManager.EXTRA_MEDIA, mediaWrapper);
        }
        PendingIntent contentPendingIntent =
                stackBuilder.getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT);

        int pauseOrStopResourceId = 0;
        if (info.getStreamType() == MediaInfo.STREAM_TYPE_LIVE) {
            pauseOrStopResourceId = R.drawable.ic_notification_stop_48dp;
        } else {
            pauseOrStopResourceId = R.drawable.ic_notification_pause_48dp;
        }
        int pauseOrPlayTextResourceId = isPlaying ? R.string.ccl_pause : R.string.ccl_play;

        NotificationCompat.Builder builder
                = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_action_notification)
                .setContentTitle(metadata.getString(MediaMetadata.KEY_TITLE))
                .setContentText(castingTo)
                .setContentIntent(contentPendingIntent)
                .setLargeIcon(bitmap)
                .addAction(isPlaying ? pauseOrStopResourceId
                                : R.drawable.ic_notification_play_48dp,
                        getString(pauseOrPlayTextResourceId), playbackPendingIntent)
                .addAction(R.drawable.ic_notification_disconnect_24dp,
                        getString(R.string.ccl_disconnect),
                        stopPendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)
                        .setMediaSession(mCastManager.getMediaSessionCompatToken()))
                .setOngoing(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC);


        mNotification = builder.build();

    }

    private void togglePlayback() {
        try {
            mCastManager.togglePlayback();
        } catch (Exception e) {
            LOGE(TAG, "Failed to toggle the playback", e);
        }
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

    /*
     * Reads application ID and target activity from preference storage.
     */
    private void readPersistedData() {
        String targetName = mCastManager.getPreferenceAccessor().getStringFromPreference(
                VideoCastManager.PREFS_KEY_CAST_ACTIVITY_NAME);
        try {
            if (targetName != null) {
                mTargetActivity = Class.forName(targetName);
            } else {
                mTargetActivity = VideoCastControllerActivity.class;
            }

        } catch (ClassNotFoundException e) {
            LOGE(TAG, "Failed to find the targetActivity class", e);
        }
    }
}
