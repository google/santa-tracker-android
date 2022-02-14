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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Space
import java.util.Stack

class ViewPool(private val context: Context) {

    private val backgrounds = Stack<BackgroundViewHolder>()
    private val foregrounds = Stack<ForegroundViewHolder>()
    private val obstacles = Stack<ObstacleViewHolder>()
    private val gifts = Stack<GiftViewHolder>()
    private val spaces = Stack<SpaceViewHolder>()

    fun obtainBackground(): BackgroundViewHolder {
        if (backgrounds.isNotEmpty()) {
            return backgrounds.pop()
        }
        val image = ImageView(context)
        image.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val holder = BackgroundViewHolder(image)
        image.setTag(R.id.tag_holder, holder)
        return holder
    }

    fun obtainForeground(): ForegroundViewHolder {
        if (foregrounds.isNotEmpty()) {
            return foregrounds.pop()
        }
        val image = ImageView(context)
        image.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val holder = ForegroundViewHolder(image)
        image.setTag(R.id.tag_holder, holder)
        return holder
    }

    fun obtainObstacle(parent: ViewGroup): ObstacleViewHolder {
        if (obstacles.isNotEmpty()) {
            return obstacles.pop()
        }
        val obstacle = LayoutInflater.from(context)
                .inflate(R.layout.obstacle_layout, parent, false) as RelativeLayout
        val holder = ObstacleViewHolder(obstacle)
        obstacle.setTag(R.id.tag_holder, holder)
        return holder
    }

    fun obtainGift(): GiftViewHolder {
        if (gifts.isNotEmpty()) {
            return gifts.pop().apply { frame.visibility = View.VISIBLE }
        }
        val iv = ImageView(context)
        iv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val frame = FrameLayout(context)
        val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        frame.addView(iv, lp)
        val holder = GiftViewHolder(frame)
        frame.setTag(R.id.tag_holder, holder)
        return holder
    }

    fun obtainSpace(): SpaceViewHolder {
        if (spaces.isNotEmpty()) {
            return spaces.pop()
        }
        val space = Space(context)
        val holder = SpaceViewHolder(space)
        space.setTag(R.id.tag_holder, holder)
        return holder
    }

    fun recycle(parent: ViewGroup, view: View) {
        parent.removeView(view)
        val holder = view.getTag(R.id.tag_holder) as? ViewHolder
        when (holder) {
            is BackgroundViewHolder -> backgrounds.push(holder)
            is ForegroundViewHolder -> foregrounds.push(holder)
            is ObstacleViewHolder -> obstacles.push(holder)
            is GiftViewHolder -> gifts.push(holder)
            is SpaceViewHolder -> spaces.push(holder)
            else -> {
            }
        }
    }
}

sealed class ViewHolder(val view: View)

class BackgroundViewHolder(val image: ImageView) : ViewHolder(image)
class ForegroundViewHolder(val image: ImageView) : ViewHolder(image)

class ObstacleViewHolder(val obstacle: RelativeLayout) : ViewHolder(obstacle) {
    val top: ImageView = obstacle.findViewById(R.id.top_view)
    val back: ImageView = view.findViewById(R.id.back_view)
    val bottom: ImageView = view.findViewById(R.id.bottom_view)
}

class GiftViewHolder(val frame: FrameLayout) : ViewHolder(frame) {
    val image = frame.getChildAt(0) as ImageView
}

class SpaceViewHolder(val space: Space) : ViewHolder(space)
