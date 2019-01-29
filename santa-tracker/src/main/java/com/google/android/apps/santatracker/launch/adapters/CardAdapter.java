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

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.launch.AbstractLaunch;
import com.google.android.apps.santatracker.launch.CardLayoutManager;
import com.google.android.apps.santatracker.launch.LaunchCollection;
import com.google.android.apps.santatracker.launch.LauncherDataChangedCallback;
import com.google.android.apps.santatracker.launch.SantaContext;
import com.google.android.flexbox.FlexboxLayoutManager;

public class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements LauncherDataChangedCallback {

    private static final int HEADER_VIEW_TYPE = 0;
    private static final int ITEM_VIEW_TYPE = 1;

    private static final int[] LOCKED_COLORS =
            new int[] {
                R.color.SantaTranslucentPurple,
                R.color.SantaTranslucentBlue,
                R.color.SantaTranslucentYellow,
                R.color.SantaTranslucentGreen,
                R.color.SantaTranslucentRed,
            };

    private SantaContext mContext;
    @Nullable LaunchCollection mLaunchers;
    @StringRes private int mHeaderTextRes;

    public CardAdapter(SantaContext santaContext, @StringRes int headerTextRes) {
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
            case ITEM_VIEW_TYPE:
                bindItemViewHolder((ItemViewHolder) holder, position);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void setLaunchers(LaunchCollection launchers) {
        mLaunchers = launchers;
        mLaunchers.updateVisibleList();
    }

    void bindItemViewHolder(ItemViewHolder holder, int position) {
        int lockedLayerColor =
                ContextCompat.getColor(
                        holder.itemView.getContext(), getLockedViewResource(position));
        AbstractLaunch launcher = mLaunchers.getVisibleLauncherFromPosition(position);
        holder.setLauncher(launcher, lockedLayerColor, launcher.isLocked());
        boolean isFeatured = isCardFeatured(position);
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            Resources resources = mContext.getResources();
            if (isFeatured) {
                flexboxLp.setFlexBasisPercent(0.4f);
                int minWidth =
                        resources.getDimensionPixelSize(R.dimen.village_card_featured_min_width);
                int space = resources.getDimensionPixelSize(R.dimen.spacing_extra_large);
                flexboxLp.setMinWidth(minWidth);
                holder.itemView.setPadding(space, 0, space, space);
            } else {
                flexboxLp.setFlexBasisPercent(0.24f);
                int minWidth =
                        resources.getDimensionPixelSize(
                                R.dimen.village_card_non_featured_min_width);
                int maxWidth = resources.getDimensionPixelSize(R.dimen.village_card_max_width);
                flexboxLp.setMinWidth(minWidth);
                flexboxLp.setMaxWidth(maxWidth);
                int space = resources.getDimensionPixelSize(R.dimen.spacing_large);
                holder.itemView.setPadding(space, 0, space, space);
            }
            flexboxLp.setFlexGrow(1f);
            holder.itemView.setLayoutParams(flexboxLp);
        }
        holder.launcher.setLockedView(holder.lockedView);
        holder.launcher.applyState();
        holder.launcher.setImageView(holder.backgroundImageView);
        ViewCompat.setTransitionName(holder.backgroundImageView, holder.launcher.getTitle());
    }

    @Override
    public int getItemCount() {
        if (mLaunchers != null) {
            return mLaunchers.getNumVisibleLaunchers();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return HEADER_VIEW_TYPE;
        } else {
            return ITEM_VIEW_TYPE;
        }
    }

    @Override
    public void refreshData() {
        if (mLaunchers != null) {
            mLaunchers.updateVisibleList();
        }
        notifyDataSetChanged();
    }

    public boolean isCardFeatured(int position) {
        return mLaunchers.getVisibleLauncherFromPosition(position).isFeatured();
    }

    @ColorRes
    private int getLockedViewResource(int position) {
        int lockedColor =
                mLaunchers.getVisibleLauncherFromPosition(position).getLockedViewResource();
        if (lockedColor != -1) {
            return lockedColor;
        } else {
            return LOCKED_COLORS[position % LOCKED_COLORS.length];
        }
    }

    public static class ColumnCountCalculator {
        private int nonFeatureColumnSize;
        private int featureColumnSize;
        private LaunchCollection launches;

        public ColumnCountCalculator(
                int nonFeatureColumnSize, int featureColumnSize, LaunchCollection launches) {
            this.launches = launches;
            this.nonFeatureColumnSize = nonFeatureColumnSize;
            this.featureColumnSize = featureColumnSize;
        }

        public int calculateColumnCount(int position) {
            return CardLayoutManager.SPAN_COUNT
                    / (launches.getVisibleLauncherFromPosition(position).isFeatured()
                            ? featureColumnSize
                            : nonFeatureColumnSize);
        }
    }
}
