/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.launch;

import android.support.annotation.ColorRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.FontHelper;

import java.util.ArrayList;
import java.util.List;

public abstract class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder>
        implements LauncherDataChangedCallback {

    private static final int[] LOCKED_COLORS = new int[]{
            R.color.SantaTranslucentPurple,
            R.color.SantaTranslucentBlue,
            R.color.SantaTranslucentYellow,
            R.color.SantaTranslucentGreen,
            R.color.SantaTranslucentRed,
    };

    protected SantaContext mContext;
    protected AbstractLaunch[] mAllLaunchers;
    protected AbstractLaunch[] mLaunchers;

    public CardAdapter(SantaContext santaContext, AbstractLaunch[] launchers) {
        mContext = santaContext;
        mAllLaunchers = launchers;
        initializeLaunchers(santaContext);
        updateMarkerVisibility();
    }

    public abstract void initializeLaunchers(SantaContext santaContext);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.layout_village_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setLauncher(mLaunchers[position]);

        holder.lockedView.setBackgroundResource(getLockedViewResource(position));
        holder.launcher.setLockedView(holder.lockedView);
        holder.launcher.applyState();
    }

    @Override
    public int getItemCount() {
        return mLaunchers.length;
    }

    @Override
    public void refreshData() {
        updateMarkerVisibility();
        notifyDataSetChanged();
    }

    @ColorRes
    public int getLockedViewResource(int position) {
        return LOCKED_COLORS[position % LOCKED_COLORS.length];
    }

    public void updateMarkerVisibility() {
        List<AbstractLaunch> launchers = new ArrayList<>(mAllLaunchers.length);
        for (int i = 0; i < mAllLaunchers.length; i++) {
            if (mAllLaunchers[i].getState() != AbstractLaunch.STATE_HIDDEN) {
                launchers.add(mAllLaunchers[i]);
            }
        }
        mLaunchers = launchers.toArray(new AbstractLaunch[launchers.size()]);
    }

    public AbstractLaunch[] getLaunchers() {
        return mAllLaunchers;
    }

    public AbstractLaunch getLauncher(int i) {
        return mAllLaunchers[i];
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public AbstractLaunch launcher;
        public ImageView backgroundImageView;
        public TextView nameView;
        public TextView verbView;
        public View lockedView;

        public ViewHolder(View itemView) {
            super(itemView);

            backgroundImageView = (ImageView) itemView.findViewById(R.id.card_background_image);
            nameView = (TextView) itemView.findViewById(R.id.card_name_text);
            verbView = (TextView) itemView.findViewById(R.id.card_verb);
            lockedView = itemView.findViewById(R.id.card_disabled);
        }

        public void setLauncher(AbstractLaunch launcher) {
            this.launcher = launcher;

            // Loading all of these beautiful images at full res is laggy without using
            // Glide, however this makes it asynchronous.  We should consider either compromising
            // on image resolution or doing some sort of nifty placeholder.
            Glide.with(itemView.getContext())
                    .fromResource()
                    .centerCrop()
                    .placeholder(R.color.disabledMarker)
                    .load(launcher.getCardResource())
                    .into(backgroundImageView);

            nameView.setText(launcher.getContentDescription());
            verbView.setText(launcher.getVerb());
            FontHelper.makeLobster(verbView);
            itemView.setContentDescription(launcher.getContentDescription());

            launcher.attachToView(this.itemView);
        }
    }
}
