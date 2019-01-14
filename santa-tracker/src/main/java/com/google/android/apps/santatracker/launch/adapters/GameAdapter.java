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

package com.google.android.apps.santatracker.launch.adapters;

import static com.google.android.apps.santatracker.launch.CardKeys.WEB_AIRPORT_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_BOATLOAD_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_CLAUSDRAWS_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_CODEBOOGIE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_CODELAB_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_ELFMAKER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_ELFSKI_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_GUMBALL_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_JAMBAND_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_PENGUINDASH_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_PRESENTBOUNCE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_PRESENTDROP_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_RACER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_RUNNER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SANTASEARCH_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SANTASELFIE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SEASONOFGIVING_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SNOWBALL_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SNOWFLAKE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SPEEDSKETCH_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_WRAPBATTLE_CARD;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.config.WebConfig;
import com.google.android.apps.santatracker.launch.SantaContext;
import com.google.android.flexbox.FlexboxLayoutManager;

// TODO: Consider smarter way to reuse the shared code between CardAdapter than extending it
public class GameAdapter extends CardAdapter {

    private static final int HEADER_VIEW_TYPE = 0;
    private static final int ITEM_VIEW_TYPE = 1;
    private static final int FOOTER_VIEW_TYPE = 2;

    public static final SparseArray<String> WEB_CONFIG_MAPPING;

    static {
        // Map a card to a configuration option
        WEB_CONFIG_MAPPING = new SparseArray<>();
        WEB_CONFIG_MAPPING.put(WEB_AIRPORT_CARD, WebConfig.AIRPORT);
        WEB_CONFIG_MAPPING.put(WEB_BOATLOAD_CARD, WebConfig.BOATLOAD);
        WEB_CONFIG_MAPPING.put(WEB_CLAUSDRAWS_CARD, WebConfig.CLAUSDRAWS);
        WEB_CONFIG_MAPPING.put(WEB_CODEBOOGIE_CARD, WebConfig.CODEBOOGIE);
        WEB_CONFIG_MAPPING.put(WEB_CODELAB_CARD, WebConfig.CODELAB);
        WEB_CONFIG_MAPPING.put(WEB_GUMBALL_CARD, WebConfig.GUMBALL);
        WEB_CONFIG_MAPPING.put(WEB_JAMBAND_CARD, WebConfig.JAMBAND);
        WEB_CONFIG_MAPPING.put(WEB_PENGUINDASH_CARD, WebConfig.PENGUINDASH);
        WEB_CONFIG_MAPPING.put(WEB_PRESENTBOUNCE_CARD, WebConfig.PRESENTBOUNCE);
        WEB_CONFIG_MAPPING.put(WEB_PRESENTDROP_CARD, WebConfig.PRESENTDROP);
        WEB_CONFIG_MAPPING.put(WEB_RACER_CARD, WebConfig.RACER);
        WEB_CONFIG_MAPPING.put(WEB_RUNNER_CARD, WebConfig.RUNNER);
        WEB_CONFIG_MAPPING.put(WEB_SANTASEARCH_CARD, WebConfig.SANTASEARCH);
        WEB_CONFIG_MAPPING.put(WEB_SANTASELFIE_CARD, WebConfig.SANTASELFIE);
        WEB_CONFIG_MAPPING.put(WEB_SEASONOFGIVING_CARD, WebConfig.SEASONOFGIVING);
        WEB_CONFIG_MAPPING.put(WEB_ELFSKI_CARD, WebConfig.ELFSKI);
        WEB_CONFIG_MAPPING.put(WEB_SNOWBALL_CARD, WebConfig.SNOWBALL);
        WEB_CONFIG_MAPPING.put(WEB_SNOWFLAKE_CARD, WebConfig.SNOWFLAKE);
        WEB_CONFIG_MAPPING.put(WEB_SPEEDSKETCH_CARD, WebConfig.SPEEDSKETCH);
        WEB_CONFIG_MAPPING.put(WEB_WRAPBATTLE_CARD, WebConfig.WRAPBATTLE);
        WEB_CONFIG_MAPPING.put(WEB_ELFMAKER_CARD, WebConfig.ELFMAKER);
    }

    private SantaContext mContext;
    @StringRes private int mHeaderTextRes;

    public GameAdapter(SantaContext santaContext, @StringRes int headerTextRes) {
        super(santaContext, headerTextRes);
        mContext = santaContext;
        mHeaderTextRes = headerTextRes;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView;
        switch (viewType) {
            case HEADER_VIEW_TYPE:
                itemView = inflater.inflate(R.layout.layout_village_header, parent, false);
                holder =
                        new HeaderViewHolder(
                                itemView, mContext.getResources().getString(mHeaderTextRes));
                break;
            case ITEM_VIEW_TYPE:
                itemView = inflater.inflate(R.layout.layout_village_card, parent, false);
                holder = new ItemViewHolder(itemView);
                break;
            case FOOTER_VIEW_TYPE:
                itemView =
                        inflater.inflate(R.layout.layout_village_footer_new_games, parent, false);
                holder = new FooterViewHolder(itemView);
                break;
            default:
                throw new IllegalArgumentException();
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case HEADER_VIEW_TYPE:
                break;
            case FOOTER_VIEW_TYPE:
                bindFooterViewHolder((FooterViewHolder) holder);
                break;
            case ITEM_VIEW_TYPE:
                bindItemViewHolder((ItemViewHolder) holder, position);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void bindFooterViewHolder(FooterViewHolder holder) {
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            flexboxLp.setFlexBasisPercent(0.9f);
            flexboxLp.setFlexGrow(1f);
            holder.itemView.setLayoutParams(flexboxLp);
        }
    }

    @Override
    public int getItemCount() {
        // Add +1 for the footer
        return mLaunchers.getNumVisibleLaunchers() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return HEADER_VIEW_TYPE;
        } else if (position == mLaunchers.getNumVisibleLaunchers()) {
            return FOOTER_VIEW_TYPE;
        } else {
            return ITEM_VIEW_TYPE;
        }
    }

    private static class FooterViewHolder extends RecyclerView.ViewHolder {
        FooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}
