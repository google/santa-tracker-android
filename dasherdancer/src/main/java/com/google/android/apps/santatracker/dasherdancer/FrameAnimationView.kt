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
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class FrameAnimationView : AppCompatImageView {

    private var frames: Array<Drawable?>? = null

    private var frameIndices: IntArray? = null
    var frameIndex: Int = 0
        set(frameIndex) {
            field = frameIndex
            val frames = frames ?: return
            val frameIndices = frameIndices ?: return
            if (this.frameIndex >= 0 &&
                    this.frameIndex < frameIndices.size &&
                    frames[frameIndices[this.frameIndex]] != null &&
                    !isBitmapRecycled(frames[frameIndices[this.frameIndex]])) {
                invalidate()
            }
        }
    private val paint = Paint()
    private val insetDrawable: InsetDrawableCompat? = null

    internal var matrix = Matrix()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        paint.isAntiAlias = true
        setImageDrawable(insetDrawable)
    }

    /**
     * Will attempt to recycle the old frames before setting the new frames.
     *
     * @param frames
     * @param frameIndices
     */
    fun setFrames(frames: Array<Drawable?>?, frameIndices: IntArray?) {
        if (this.frames != null) {
            this.frames = null
        }
        this.frames = frames
        this.frameIndices = frameIndices
    }

    private fun isBitmapRecycled(drawable: Drawable?): Boolean {
        var drawable = drawable
        if (drawable != null && drawable is InsetDrawableCompat) {
            drawable = drawable.drawable
        }
        if (drawable != null && drawable is BitmapDrawable) {
            val bmp = drawable.bitmap
            if (bmp != null) {
                return bmp.isRecycled
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateMatrix(w, h)
    }

    private fun recalculateMatrix(vwidth: Int, vheight: Int) {
        val frames = frames ?: return
        val frameIndices = frameIndices ?: return
        if (frameIndex >= 0 && frameIndex < frameIndices.size) {
            val insetDrawableCompat = frames[frameIndices[frameIndex]] as InsetDrawableCompat

            if (isBitmapRecycled(insetDrawableCompat)) {
                return
            }

            matrix.reset()
            val scale: Float
            var dx = 0f
            var dy = 0f

            val dwidth = (insetDrawableCompat.drawable!!.intrinsicWidth +
                    insetDrawableCompat.left +
                    insetDrawableCompat.right)
            val dheight = (insetDrawableCompat.drawable!!.intrinsicHeight +
                    insetDrawableCompat.top +
                    insetDrawableCompat.bottom)

            if (dwidth * vheight > vwidth * dheight) {
                scale = vheight.toFloat() / dheight.toFloat()
                dx = (vwidth - dwidth * scale) * 0.5f
            } else {
                scale = vwidth.toFloat() / dwidth.toFloat()
                dy = (vheight - dheight * scale) * 0.5f
            }

            matrix.setTranslate(insetDrawableCompat.left.toFloat(), insetDrawableCompat.top.toFloat())
            matrix.postScale(scale, scale)
            matrix.postTranslate(Math.round(dx).toFloat(), Math.round(dy).toFloat())
            imageMatrix = matrix
        }
    }

    public override fun onDraw(c: Canvas) {
        val frames = frames ?: return
        val frameIndices = frameIndices ?: return
        if (frameIndex >= 0 && frameIndex < frameIndices.size) {
            // the line below should work with InsetDrawable with CENTER_CROP,
            // but it doesn't work on older APIs (JB) beacause of different handling of insets:
            // setImageDrawable(frames[frameIndices[mFrameIndex]]);

            // code below fixes the bug in InsetDrawable and works on all API levels:
            // instead of setting the InsetDrawable on FrameAnimationView,
            // we set the Bitmap directly and use the insets to calculate the correct matrix

            val insetDrawableCompat = frames[frameIndices[frameIndex]] as InsetDrawableCompat
            if (isBitmapRecycled(insetDrawableCompat)) {
                return
            }
            val newBitmap = (insetDrawableCompat.drawable as BitmapDrawable).bitmap

            val current = drawable
            if (current == null || current is BitmapDrawable && newBitmap != current.bitmap) {
                setImageBitmap(newBitmap)
                recalculateMatrix(width, height)
            }
        }

        if (isBitmapRecycled(drawable)) {
            return
        }
        super.onDraw(c)
    }
}
