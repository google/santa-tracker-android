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

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.google.android.apps.santatracker.util.SantaLog

class CharacterAdapter(private val characters: Array<Character>) : PagerAdapter() {

    private val views: SparseArray<ImageView> = SparseArray()

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        if (position < characters.size) {
            var view: View? = views.get(position)
            if (view == null) {
                try {
                    view = `object` as ImageView
                } catch (e: ClassCastException) {
                    SantaLog.e(LOG_TAG, "Wrong type of object supplied to destroyItem.")
                }
            }

            if (view != null) {
                container.removeView(view)
            }

            views.remove(position)
        }
    }

    override fun getCount(): Int {
        return characters.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        var view: ImageView? = null

        if (position < characters.size) {
            if (views.get(position) != null) {
                view = views.get(position)
            } else {
                view = FrameAnimationView(container.context)
                view.scaleType = ImageView.ScaleType.MATRIX
                // Load the first idle frame.
                view.setBackgroundResource(mBackgrounds[position])
                view.tag = position
                views.put(position, view)
            }

            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            container.addView(view, lp)
        }

        return view!!
    }

    override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
        return arg0 === arg1
    }

    companion object {

        private val LOG_TAG = CharacterAdapter::class.java.simpleName

        private val mBackgrounds = intArrayOf(R.color.bg_blue, R.color.bg_pink, R.color.bg_purple, R.color.bg_green, R.color.bg_red, R.color.bg_orange, R.color.bg_yellow)
    }
}
