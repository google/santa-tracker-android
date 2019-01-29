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

package com.google.android.apps.santatracker.util

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.view.accessibility.AccessibilityEventCompat

/** Utility methods for accessibility support.  */
object AccessibilityUtil {

    /** Return true if the accessibility service or touch exploration are enabled.  */
    @JvmStatic
    fun isTouchAccessiblityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val isAccessibilityEnabled = am.isEnabled
        val isTouchExplorationEnabled = am.isTouchExplorationEnabled
        return isAccessibilityEnabled || isTouchExplorationEnabled
    }

    /**
     * Announce text through the AccessibilityManager for a view.
     *
     * @param text
     * @param view
     * @param manager
     */
    @JvmStatic
    fun announceText(text: String, view: View, manager: AccessibilityManager) {
        // Only announce text if the accessibility service is enabled
        if (!manager.isEnabled) {
            return
        }

        val event = AccessibilityEvent.obtain(
                AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
        event.text.add(text)
        event.isEnabled = true
        // Tie the event to the view
        event.className = view.javaClass.name
        event.packageName = view.context.packageName
        event.setSource(view)

        // Send the announcement
        manager.sendAccessibilityEvent(event)
    }
}
