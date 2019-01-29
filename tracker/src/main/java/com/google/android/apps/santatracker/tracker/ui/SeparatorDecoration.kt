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
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.apps.santatracker.tracker.R

class SeparatorDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val drawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.stream_separator)
    private val height: Int = context.resources.getDimensionPixelSize(
            com.google.android.apps.santatracker.common.R.dimen.separator_height)

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (child.alpha < 0.8f) {
                continue
            }
            val params = child.layoutParams as RecyclerView.LayoutParams
            val translationY = child.translationY.toInt()
            val top = child.bottom + params.bottomMargin + translationY
            val bottom = top + height
            drawable?.apply {
                setBounds(left, top, right, bottom)
                draw(c)
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.set(0, 0, 0, height)
    }
}
