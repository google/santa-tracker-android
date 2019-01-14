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
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.LocaleListCompat

/**
 * A special version of [AppCompatTextView] that replaces spaces with line breaks in order to
 * explicitly adjust the position of line breaks. Currently, this behavior is only for Japanese and
 * Korean. In other locales, this view only sets [Layout.BREAK_STRATEGY_HIGH_QUALITY].
 */
class TitleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
        AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        if (Build.VERSION.SDK_INT >= 23) {
            breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val tags = LocaleListCompat.getAdjustedDefault().toLanguageTags()
        if (tags.startsWith("ja") || tags.startsWith("ko")) {
            transformationMethod = TitleTransformationMethod(w)
        }
    }

    private class TitleTransformationMethod internal constructor(private val mWidth: Int) :
            TransformationMethod {

        override fun getTransformation(source: CharSequence, view: View): CharSequence {
            if (source is Spannable) {
                return source
            }
            val builder = StringBuilder()
            val textView = view as TextView
            val paint = textView.paint
            var head = 0
            var tail = 0 // The position of the last possible line break
            val length = source.length
            for (i in 0 until length) {
                val c = source[i]
                if (isLineBreak(c)) {
                    if (mWidth < Layout.getDesiredWidth(source, head, i, paint)) { // Don't fit
                        if (head == tail) { // This chunk of text is too long to fit in one line
                            // Just give up and let the system handle it
                            return source
                        } else {
                            builder.append(source, head, tail)
                            builder.append('\n')
                            tail += 1
                            head = tail
                        }
                    } else {
                        tail = i
                    }
                }
            }
            if (mWidth < Layout.getDesiredWidth(source, head, length, paint) && head < tail) {
                builder.append(source, head, tail)
                builder.append('\n')
                head = tail + 1
            }
            builder.append(source, head, length)
            return builder
        }

        private fun isLineBreak(c: Char): Boolean {
            return c == ' '
        }

        override fun onFocusChanged(
            view: View,
            sourceText: CharSequence,
            focused: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect
        ) {
            // Do nothing
        }
    }
}
