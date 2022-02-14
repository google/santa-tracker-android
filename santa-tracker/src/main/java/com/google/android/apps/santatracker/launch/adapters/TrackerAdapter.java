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

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.launch.AbstractLaunch;
import com.google.android.apps.santatracker.launch.LaunchSantaTracker;
import com.google.android.apps.santatracker.launch.LauncherDataChangedCallback;
import com.google.android.flexbox.FlexboxLayoutManager;

public class TrackerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements LauncherDataChangedCallback {

    private LaunchSantaTracker mLaunchSantaTracker;

    public void setLauncher(LaunchSantaTracker launchSantaTracker) {
        mLaunchSantaTracker = launchSantaTracker;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.layout_village_tracker_card, parent, false);
        return new TrackerCardViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        bindItemViewHolder((TrackerCardViewHolder) holder);
    }

    private void bindItemViewHolder(TrackerCardViewHolder holder) {
        holder.setLauncher(mLaunchSantaTracker, mLaunchSantaTracker.isLocked());
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            flexboxLp.setFlexBasisPercent(0.9f);
            flexboxLp.setFlexGrow(1f);
            holder.itemView.setLayoutParams(flexboxLp);
        }
        holder.launcher.setLockedView(holder.lockedView);
        holder.launcher.applyState();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public void refreshData() {
        notifyDataSetChanged();
    }

    private static class TrackerCardViewHolder extends RecyclerView.ViewHolder {

        AbstractLaunch launcher;
        View lockedView;
        ImageView backgroundImageView;
        ImageView lockedLayerImageView;
        TextView verbView;
        View squareFrame;

        TrackerCardViewHolder(View itemView) {
            super(itemView);
            this.backgroundImageView = itemView.findViewById(R.id.card_background_image);
            this.verbView = itemView.findViewById(R.id.track_santa);
            this.squareFrame = itemView.findViewById(R.id.square_frame);
            this.lockedView = itemView.findViewById(R.id.card_disabled);
            this.lockedLayerImageView = itemView.findViewById(R.id.locked_layer);
        }

        void setLauncher(AbstractLaunch abstractLaunch, boolean isLocked) {
            this.launcher = abstractLaunch;
            Context context = itemView.getContext();
            Glide.with(context)
                    .load(launcher.getCardDrawableRes())
                    .apply(centerCropTransform())
                    .into(backgroundImageView);
            if (isLocked) {
                // ColorTransformation is not required because the R.drawable.locked_layer should
                // the color for the locked layer for the tracker
                Glide.with(context)
                        .load(R.drawable.locked_layer)
                        .apply(centerCropTransform())
                        .into(lockedLayerImageView);
            }
            launcher.attachToView(squareFrame);
        }
    }
}
