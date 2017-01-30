/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.google.android.apps.santatracker;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.util.SantaLog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import static com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME;

/**
 * A {@link com.google.android.gms.wearable.WearableListenerService} that is invoked when the
 * state of synced phone and wear notifications changes:
 * - if a notification has been dismissed on the wearable, onDataChanged is called
 *
 * - if a notification should be shown on the wearable, so the DataApi should be updated
 * - if a notification should be dismissed on the wearable, so the DataApi should be updated
 */
public class PhoneNotificationService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DeleteDataItemsResult> {

    private final String TAG = "PhoneNotification";
    private GoogleApiClient mGoogleApiClient;
    private Intent mIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
                // A notification has been deleted on the watch and it modified the DataApi to
                // notify us.
                // Only one notification shown at a time, so dismiss it
                NotificationManagerCompat.from(this).cancelAll();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        if (null != intent) {
            mIntent = intent;
            mGoogleApiClient.connect();
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override // ConnectionCallbacks
    public void onConnected(Bundle bundle) {
        SantaLog.d(TAG, "onConnected,  action = " + mIntent.getAction());

        if (mIntent.getAction().equals(NotificationConstants.ACTION_DISMISS)) {

            final Uri dataItemUri =
                    new Uri.Builder().scheme(WEAR_URI_SCHEME).build();
            SantaLog.d(TAG, "Deleting Uri: " + dataItemUri.toString());

            Wearable.DataApi.deleteDataItems(
                    mGoogleApiClient, dataItemUri).setResultCallback(this);

        } else if (mIntent.getAction().equals(NotificationConstants.ACTION_SEND)) {
            requestWearableNotification(
                    mIntent.getStringExtra(NotificationConstants.KEY_CONTENT),
                    NotificationConstants.TAKEOFF_PATH);
        }
    }


    /**
     * Builds a DataItem that on the wearable will be interpreted as a request to show a
     * notification. The result will be a notification that only shows up on the wearable.
     */
    private void requestWearableNotification(String content, String path) {
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
            putDataMapRequest.getDataMap().putString(NotificationConstants.KEY_CONTENT, content);

            //Ensure data item is unique
            putDataMapRequest.getDataMap().putLong("time", System.currentTimeMillis());
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "buildWatchOnlyNotification(): Failed to set the data, "
                                        + "status: " + dataItemResult.getStatus().getStatusCode());
                            } else {
                                SantaLog.e(TAG, "takeoff notification: " + dataItemResult.getStatus()
                                        .getStatusCode());
                            }
                            mGoogleApiClient.disconnect();
                        }
                    });
        } else {
            Log.e(TAG, "Can't send data item: no Google API Client connection");
        }
    }

    @Override // ConnectionCallbacks
    public void onConnectionSuspended(int i) {
    }

    @Override // OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to the Google API client");
    }

    @Override // ResultCallback<DataApi.DeleteDataItemsResult>
    public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
        if (!deleteDataItemsResult.getStatus().isSuccess()) {
            Log.e(TAG, "dismissWearableNotification(): failed to delete DataItem");
        }
        mGoogleApiClient.disconnect();
    }
}
