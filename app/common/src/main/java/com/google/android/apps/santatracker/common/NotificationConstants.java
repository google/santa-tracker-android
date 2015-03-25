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

package com.google.android.apps.santatracker.common;

/**
 * Constants that define notifications for the application and wearable.
 */
public final class NotificationConstants {

    public static final String KEY_LOCATION = "location";

    private NotificationConstants() {
    }

    ;

    // Only one ID because we only show one notification at a time.

    /**
     * ID that identifies all notifications from Santa Tracker.
     * Reusing the same ID ensures that only one notification is shown at a time.
     */
    public static final int NOTIFICATION_ID = 4672682; // GOSANTA

    /**
     * Take-off notification.
     */
    public static final int NOTIFICATION_TAKEOFF = 1;

    /**
     * Santa status update or factoid notification.
     */
    public static final int NOTIFICATION_INFO = 2;

    /**
     * Path to identify take-off notifications on wear.
     */
    public static final String TAKEOFF_PATH = "/takeoff";

    /**
     * Key for notification ID.
     *
     * @see #NOTIFICATION_ID
     */
    public static final String KEY_NOTIFICATION_ID = "notification-id";

    /**
     * Key for the type of notification.
     *
     * @see #NOTIFICATION_TAKEOFF
     * @see #NOTIFICATION_INFO
     */
    public static final String KEY_NOTIFICATION_TYPE = "notification-type";

    /**
     * Key for the title in a notification.
     */
    public static final String KEY_TITLE = "title";

    /**
     * Key for the content text in a notification.
     */
    public static final String KEY_CONTENT = "content";

    /**
     * Key for the timestamp of a notification.
     */
    public static final String KEY_TIMESTAMP = "timestap";

    /**
     * Key for Santa's final arrival (after which the notification should not be displayed.)
     */
    public static final String KEY_FINAL_ARRIVAL = "finalArrival";

    /**
     * Key for the fact text in a notification.
     */
    public static final String KEY_FACT = "fact";

    /**
     * Key for Santa's status in a notification.
     */
    public static final String KEY_STATUS = "status";

    /**
     * Key for the image in a notification.
     */
    public static final String KEY_IMAGEURL = "imageurl";

    /**
     * Action string that identifies a dismiss action.
     */
    public static final String ACTION_DISMISS
            = "com.google.android.apps.santatracker.DISMISS";
}
