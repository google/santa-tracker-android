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

import com.google.android.apps.santatracker.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.apps.santatracker.common.NotificationConstants;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Gravity;

import static com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME;

/**
 * A {@link com.google.android.gms.wearable.WearableListenerService} that will be invoked when a
 * DataItem is added or deleted. The creation of a new DataItem will be interpreted as a request to
 * create a new notification and the removal of that DataItem is interpreted as a request to
 * dismiss that notification.
 */
public class WearNotificationService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DeleteDataItemsResult> {

    private static final String TAG = "NotificationUpdate";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate()");
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * Dismisses the phone notification, via a {@link android.app.PendingIntent} that is triggered
     * when the user dismisses the local notification. Deleting the corresponding data item
     * notifies
     * the {@link com.google.android.gms.wearable.WearableListenerService} on the phone that the
     * matching notification on the phone side should be removed.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand()");

        if (null != intent) {
            String action = intent.getAction();
            if (NotificationConstants.ACTION_DISMISS.equals(action)) {
                // We need to dismiss the wearable notification. We delete the data item that
                // created the notification and that is how we inform the phone.
                mGoogleApiClient.connect();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "onDataChanged");
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String content = dataMap.getString(NotificationConstants.KEY_CONTENT);
                String path = dataEvent.getDataItem().getUri().getPath();
                if (NotificationConstants.TAKEOFF_PATH.equals(path)) {
                    Log.v(TAG, "building takeoff notification");
                    buildTakeoffNotification(content);
                }
            } else if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
                // There's only one notification shown at a time, so just dismiss it.
                NotificationManagerCompat.from(this).cancelAll();
            }
        }
    }

    /**
     * Builds a simple notification on the wearable.
     */
    private void buildTakeoffNotification(String content) {
        Log.v(TAG, "buildTakeoffNotification: " + content);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher);

        // TODO workaround for bug for always displaying notifications
        builder.setContentTitle("");
        Bitmap largeIcon = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.santa_notification_background);
        builder.setLargeIcon(largeIcon);

        //We want to know when the notification is dismissed, so we can dismiss it on the phone.
        Intent dismissIntent = new Intent(NotificationConstants.ACTION_DISMISS);
        dismissIntent.putExtra(NotificationConstants.KEY_NOTIFICATION_ID,
                NotificationConstants.NOTIFICATION_ID);
        PendingIntent pendingIntent = PendingIntent
                .getService(this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(pendingIntent);

        //Post the notification.
        NotificationManagerCompat.from(this)
                .notify(NotificationConstants.NOTIFICATION_ID, builder.build());
    }

    /**
     * The only data the wearable sends to the phone is that its notification has been dismissed,
     * so if the service has been launched by an Intent, we update the DataAPI to let the phone
     * know to clear its corresponding notification.
     */
    @Override
    public void onConnected(Bundle bundle) {
        final Uri dataItemUri = new Uri.Builder().scheme(WEAR_URI_SCHEME)
                .path(NotificationConstants.TAKEOFF_PATH).build();
        Log.d(TAG, "Deleting Uri: " + dataItemUri.toString());

        Wearable.DataApi.deleteDataItems(
                mGoogleApiClient, dataItemUri).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
        if (!deleteDataItemsResult.getStatus().isSuccess()) {
            Log.e(TAG, "dismissWearableNotification(): failed to delete DataItem");
        }
        mGoogleApiClient.disconnect();
    }
}