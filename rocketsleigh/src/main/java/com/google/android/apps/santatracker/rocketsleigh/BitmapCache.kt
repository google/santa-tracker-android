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

package com.google.android.apps.santatracker.rocketsleigh

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.SparseArray
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.containsKey
import com.google.android.apps.santatracker.util.SantaLog
import java.util.concurrent.Executor

class BitmapCache(
    private val context: Context,
    private val executor: Executor,
    private val scaleX: Float,
    private val scaleY: Float
) {

    companion object {
        private const val TAG = "BitmapCache"
    }

    private val cache = SparseArray<Bitmap>()
    private val secondaryCache = SparseArray<Bitmap>()
    private val queued = mutableListOf<Int>()

    @UiThread
    fun preload(@DrawableRes id: Int, splitSecondary: Boolean = false) {
        if (id == 0 || cache.containsKey(id) || queued.contains(id)) {
            return
        }
        queued.add(id)
        executor.execute {
            val original = AppCompatResources.getDrawable(context, id)!!.toBitmap()
            val bitmap = Bitmap.createScaledBitmap(
                    original,
                    (original.width * scaleX).toInt(),
                    (original.height * scaleY).toInt(),
                    true
            )

            if (splitSecondary) {
                val halfWidth = bitmap.width / 2
                val height = bitmap.height
                val primary = Bitmap.createBitmap(bitmap, 0, 0, halfWidth, height)
                val secondary = Bitmap.createBitmap(bitmap, halfWidth, 0, halfWidth, height)
                synchronized(cache) {
                    cache.put(id, primary)
                }
                synchronized(secondaryCache) {
                    secondaryCache.put(id, secondary)
                }
                bitmap.recycle()
            } else {
                synchronized(cache) {
                    cache.put(id, bitmap)
                }
            }
            queued.remove(id)
            SantaLog.d(TAG, "Cache size: ${cache.size()}, queued: ${queued.size}")
        }
    }

    @UiThread
    fun fetch(@DrawableRes id: Int, secondary: Boolean = false): Bitmap {
        if (id == 0) {
            throw IllegalArgumentException()
        }
        val c = if (secondary) secondaryCache else cache
        val cached = c[id]
        if (cached != null) {
            return cached
        }
        if (!queued.contains(id)) {
            preload(id, secondary)
        }
        SantaLog.w(TAG, "Synchronously waiting for a Bitmap to be cached.")
        while (!c.containsKey(id)) {
            SystemClock.sleep(10)
        }
        synchronized(c) {
            return c[id]
        }
    }

    @UiThread
    fun release(@DrawableRes id: Int, secondary: Boolean = false): Boolean {
        val c = if (secondary) secondaryCache else cache
        synchronized(c) {
            c[id]?.recycle()
            return if (c.containsKey(id)) {
                c.remove(id)
                true
            } else {
                false
            }
        }
    }

    @UiThread
    fun releaseAll() {
        listOf(cache, secondaryCache).forEach { c ->
            for (i in 0 until c.size()) {
                c.valueAt(i)?.let { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
            c.clear()
        }
    }
}
