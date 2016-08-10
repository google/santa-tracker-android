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

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.content.Context;
import android.content.Intent;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

public class NotificationDataCastManager extends DataCastManager {

    private static final String TAG = LogUtils.makeLogTag(NotificationDataCastManager.class);


    protected NotificationDataCastManager(Context context, String applicationId,
            String... namespaces) {
        super(context, applicationId, namespaces);
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String applicationStatus,
            String sessionId, boolean wasLaunched) {
        super.onApplicationConnected(appMetadata, applicationStatus, sessionId, wasLaunched);
        LOGD(TAG, "onApplicationConnected");
        startNotificationService();
    }

    @Override
    public void onApplicationDisconnected(int errorCode) {
        super.onApplicationDisconnected(errorCode);
        LOGD(TAG, "onApplicationConnected");

        stopNotificationService();
    }

    @Override
    protected void onDeviceUnselected() {
        super.onDeviceUnselected();
        LOGD(TAG, "onDeviceUnselected");

        stopNotificationService();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);
        LOGD(TAG, "onConnectionFailed");

        stopNotificationService();
    }

    private static NotificationDataCastManager sInstance;

    public static synchronized NotificationDataCastManager initialize(Context context,
            String applicationId, String... namespaces) {
        if (sInstance == null) {
            LOGD(TAG, "New instance of DataCastManager is created");
            if (ConnectionResult.SUCCESS != GooglePlayServicesUtil
                    .isGooglePlayServicesAvailable(context)) {
                String msg = "Couldn't find the appropriate version of Google Play Services";
                LOGE(TAG, msg);
                throw new RuntimeException(msg);
            }
            sInstance = new NotificationDataCastManager(context, applicationId, namespaces);
        }
        return sInstance;
    }

    public static NotificationDataCastManager getInstance() {
        if (sInstance == null) {
            String msg = "No DataCastManager instance was found, did you forget to initialize it?";
            LOGE(TAG, msg);
            throw new IllegalStateException(msg);
        }
        return sInstance;
    }

    /*
     * Starts a service that can last beyond the lifetime of the application to provide
     * notifications. The service brings itself down when needed. The service will be started only
     * if the notification feature has been enabled during the initialization.
     * @see {@link BaseCastManager#enableFeatures()}
     */
    private boolean startNotificationService() {
        LOGD(TAG, "startNotificationService()");
        Intent service = new Intent(mContext, DataCastNotificationService.class);
        service.setPackage(mContext.getPackageName());
        service.setAction(DataCastNotificationService.ACTION_VISIBILITY);
        service.putExtra(DataCastNotificationService.NOTIFICATION_VISIBILITY, !mUiVisible);
        return mContext.startService(service) != null;
    }

    private void stopNotificationService() {
        LOGD(TAG, "stopNotificationService");

        if (mContext != null) {
            mContext.stopService(new Intent(mContext, DataCastNotificationService.class));
        }
    }
}
