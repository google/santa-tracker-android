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

package com.google.android.apps.santatracker.launch;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.apps.santatracker.launch.adapters.CardAdapter;

public class CardLayoutManager extends GridLayoutManager {

    public static final String TAG = "CardLayoutManager";
    public static final int SPAN_COUNT = 12;

    public CardLayoutManager(
            Context context,
            final int nonFeaturedSpanSize,
            final int featuredSpanSize,
            final CardAdapter cardAdapter) {
        super(context, SPAN_COUNT);
        setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return cardAdapter.isCardFeatured(position)
                                ? featuredSpanSize
                                : nonFeaturedSpanSize;
                    }
                });
    }

    @Override
    public int scrollVerticallyBy(
            int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        float multiplier = 1.0f;
        float base = (float) super.scrollVerticallyBy(delta, recycler, state);

        // TODO: slow down at card borders
        return ((int) (multiplier * base));
    }
}
