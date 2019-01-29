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

package com.google.android.apps.santatracker.tracker.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.View
import com.google.android.apps.santatracker.tracker.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference

class FollowSantaButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    companion object {
        private const val MESSAGE_COUNTDOWN = 1
        private const val COUNTDOWN = 5
        private const val COUNTDOWN_DELAY_MILLIS = 55 * 1000L
    }

    private val iconSize: Int by lazy { resources.getDimensionPixelSize(R.dimen.fab_icon_size) }
    private val messageDrawables = mutableMapOf<String, Drawable>()
    private val paint = Paint()

    private var hasEnoughSpace = true
    private var isFollowingSanta = true
    private var handler: TimeoutHandler? = null

    init {
        paint.color = Color.WHITE
        paint.textSize = iconSize.toFloat()
        paint.textAlign = Paint.Align.CENTER
        scaleType = ScaleType.CENTER
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler = TimeoutHandler(this)
    }

    override fun onDetachedFromWindow() {
        handler = null
        super.onDetachedFromWindow()
    }

    /**
     * Show the specified message for the duration of time.
     *
     * @param message The message to show (needs to be very short to fit in the FAB)
     * @param duration The duration in milliseconds
     */
    fun showMessage(message: String, duration: Long) {
        val current = drawable
        setImageDrawable(getMessageDrawable(message))
        postDelayed({ setImageDrawable(current) }, duration)
    }

    fun setHasEnoughSpace(hasEnoughSpace: Boolean) {
        this.hasEnoughSpace = hasEnoughSpace
        adjustVisibility()
    }

    fun setIsFollowingSanta(isFollowingSanta: Boolean) {
        this.isFollowingSanta = isFollowingSanta
        adjustVisibility()
    }

    private fun adjustVisibility() {
        val visibility = if (hasEnoughSpace && !isFollowingSanta) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        if (getVisibility() != visibility) {
            if (visibility == View.VISIBLE) {
                show()
                handler?.apply {
                    sendMessageDelayed(Message.obtain(this, MESSAGE_COUNTDOWN, COUNTDOWN, 0),
                            COUNTDOWN_DELAY_MILLIS)
                }
            } else {
                hide()
                handler?.removeMessages(MESSAGE_COUNTDOWN)
            }
        }
    }

    private fun getMessageDrawable(message: String): Drawable {
        return messageDrawables[message] ?: BitmapDrawable(resources,
                Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_4444).also { bitmap ->
                    Canvas(bitmap).drawText(message, (iconSize / 2).toFloat(),
                            (iconSize.toFloat() - paint.descent() - paint.ascent()) / 2, paint)
                }).also { messageDrawables[message] = it }
    }

    class TimeoutHandler(button: FollowSantaButton) : Handler() {

        private val ref = WeakReference(button)

        override fun handleMessage(msg: Message?) {
            if (msg != null && msg.what == MESSAGE_COUNTDOWN) {
                val button = ref.get()
                if (button != null) {
                    val count = msg.arg1
                    if (count > 0) {
                        button.showMessage(count.toString(), 500)
                        sendMessageDelayed(Message.obtain(this, MESSAGE_COUNTDOWN, count - 1, 0),
                                1000)
                    } else if (count == 0) {
                        button.performClick()
                    }
                }
            } else {
                super.handleMessage(msg)
            }
        }
    }
}
