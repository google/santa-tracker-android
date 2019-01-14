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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

class CheckableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.imageButtonStyle
) : AppCompatImageButton(context, attrs, defStyleAttr), Checkable {

    private var _checked: Boolean = false
        set(value) {
            if (value != field) {
                refreshDrawableState()
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
            }
            field = value
        }

    init {
        ViewCompat.setAccessibilityDelegate(
                this,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
                        super.onInitializeAccessibilityEvent(host, event)
                        event.isChecked = isChecked
                    }

                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.isCheckable = true
                        info.isChecked = isChecked
                    }
                })
    }

    override fun setChecked(checked: Boolean) {
        _checked = checked
    }

    override fun isChecked(): Boolean {
        return _checked
    }

    override fun toggle() {
        _checked = !_checked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        return if (_checked) {
            View.mergeDrawableStates(
                    super.onCreateDrawableState(extraSpace + DRAWABLE_STATE_CHECKED.size),
                    DRAWABLE_STATE_CHECKED)
        } else {
            super.onCreateDrawableState(extraSpace)
        }
    }

    companion object {
        private val DRAWABLE_STATE_CHECKED = intArrayOf(android.R.attr.state_checked)
    }
}