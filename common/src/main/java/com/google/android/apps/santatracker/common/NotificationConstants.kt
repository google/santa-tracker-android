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

package com.google.android.apps.santatracker.common

/** Constants that are used in both the Application and the Wearable modules.  */
object NotificationConstants {
    const val CHANNEL_ID = "santa-general"

    // Only one ID because we only show one notification at a time.
    const val NOTIFICATION_ID = 9876435

    const val NOTIFICATION_TAKEOFF = 1

    const val TAKEOFF_PATH = "/takeoff"
    const val KEY_NOTIFICATION_ID = "notification-id"
    const val KEY_NOTIFICATION_TYPE = "notification-type"
}
