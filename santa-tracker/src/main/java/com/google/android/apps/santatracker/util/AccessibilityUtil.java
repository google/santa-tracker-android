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

package com.google.android.apps.santatracker.util;


import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Utility methods for accessibility support.
 */
public abstract class AccessibilityUtil {

    /**
     * Return true if the accessibility service or touch exploration are enabled.
     */
    public static boolean isTouchAccessiblityEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        boolean isAccessibilityEnabled = am.isEnabled();
        boolean isTouchExplorationEnabled = am.isTouchExplorationEnabled();
        return isAccessibilityEnabled || isTouchExplorationEnabled;
    }

    /**
     * Announce text through the AccessibilityManager for a view.
     *
     * @param text
     * @param view
     * @param manager
     */
    public static void announceText(String text, View view, AccessibilityManager manager) {
        // Only announce text if the accessibility service is enabled
        if (!manager.isEnabled()) {
            return;
        }

        AccessibilityEvent event = AccessibilityEvent
                .obtain(AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        event.getText().add(text);
        event.setEnabled(true);
        // Tie the event to the view
        event.setClassName(view.getClass().getName());
        event.setPackageName(view.getContext().getPackageName());
        AccessibilityEventCompat.asRecord(event).setSource(view);

        // Send the announcement
        manager.sendAccessibilityEvent(event);

    }

}
