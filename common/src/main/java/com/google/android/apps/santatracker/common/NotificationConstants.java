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

package com.google.android.apps.santatracker.common;

/**
 * Constants that are used in both the Application and the Wearable modules.
 */
public final class NotificationConstants {

    public static final String KEY_LOCATION = "location";

    private NotificationConstants() {};

    // Only one ID because we only show one notification at a time.
    public static final int NOTIFICATION_ID = 9876435;

    public static final int NOTIFICATION_TAKEOFF = 1;

    public static final String TAKEOFF_PATH = "/takeoff";
    public static final String KEY_NOTIFICATION_ID = "notification-id";
    public static final String KEY_NOTIFICATION_TYPE = "notification-type";
    public static final String KEY_TITLE = "title";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_LOCATION_PHOTO = "location-photo";
    public static final String KEY_LOCATION_MAP = "location-map";
    public static final String KEY_LOCATION_FACT = "location-fact";
    public static final String KEY_TIMESTAMP = "timestap";
    public static final String KEY_FINAL_ARRIVAL = "finalArrival";
    public static final String KEY_FACT = "fact";
    public static final String KEY_STATUS = "status";
    public static final String KEY_IMAGEURL = "imageurl";

    public static final String ACTION_DISMISS
            = "com.google.android.apps.santatracker.DISMISS";

    public static final String ACTION_SEND
            = "com.google.android.apps.santatracker.SEND";
}
