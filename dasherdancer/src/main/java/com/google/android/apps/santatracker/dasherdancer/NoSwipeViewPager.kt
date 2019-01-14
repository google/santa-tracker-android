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

package com.google.android.apps.santatracker.dasherdancer

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.OnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import androidx.viewpager.widget.ViewPager

class NoSwipeViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {

    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Take all events, as we are going to animate the current view based on touch events.
        return true
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        var retVal = false
        if (ev.pointerCount > 1) {
            retVal = scaleGestureDetector!!.onTouchEvent(ev)
        }
        if (!retVal && ev.pointerCount == 1) {
            retVal = gestureDetector!!.onTouchEvent(ev) || retVal
        }
        return retVal
    }

    fun setGestureDetectorListeners(
        context: Context,
        listener: OnGestureListener,
        scaleListener: OnScaleGestureListener
    ) {
        gestureDetector = GestureDetector(context, listener)
        scaleGestureDetector = ScaleGestureDetector(context, scaleListener)
    }
}
