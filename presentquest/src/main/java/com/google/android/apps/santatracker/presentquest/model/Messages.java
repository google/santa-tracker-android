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
package com.google.android.apps.santatracker.presentquest.model;

import android.support.annotation.StringRes;

import com.google.android.apps.santatracker.presentquest.R;

/**
 * Inventory of helper messages and how many times each one should be displayed to the user
 * over the lifetime of the app.
 */
public class Messages {

    public static class Message {

        // Unique message key (for tracking)
        public final String key;

        // ID of the resource for the message text
        @StringRes
        public final int stringId;

        // Number of times (total) that a user should see this message
        public final int timesToShow;

        public Message(String key, @StringRes int stringId, int timesToShow) {
            this.key = key;
            this.stringId = stringId;
            this.timesToShow = timesToShow;
        }
    }

    public static final Message NEW_PRESENT = new Message(
            "new_present", R.string.present_dropped, 10);

    public static final Message PRESENT_TOO_FAR = new Message(
            "present_too_far", R.string.present_too_far, Integer.MAX_VALUE);

    public static final Message WORKSHOP_TOO_FAR = new Message(
            "workshop_too_far", R.string.workshop_too_far, Integer.MAX_VALUE);

    public static final Message BAG_IS_FULL = new Message(
            "bag_is_full", R.string.bag_is_full, Integer.MAX_VALUE);

    public static final Message NO_PRESENTS_COLLECTED = new Message(
            "no_presents_collected", R.string.no_presents_collected, Integer.MAX_VALUE);

    public static final Message PRESENT_NEARBY = new Message(
            "present_nearby", R.string.msg_present_nearby, 5);

    public static final Message BAG_ALMOST_FULL = new Message(
            "bag_almost_full", R.string.msg_bag_almost_full, 5);

    public static final Message UNLOAD_BAG = new Message(
            "unload_bag", R.string.msg_unload_bag, 5);

    public static final Message CLICK_PRESENT = new Message(
            "click_present", R.string.msg_click_present, 3);

    public static final Message CLICK_WORKSHOP = new Message(
            "click_workshop", R.string.msg_click_workshop, 3);

}
