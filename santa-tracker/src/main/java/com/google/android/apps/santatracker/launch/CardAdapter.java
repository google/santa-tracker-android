/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.apps.santatracker.R;

import java.util.ArrayList;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder>
        implements LauncherDataChangedCallback {

    public static final int SANTA = 0;
    public static final int VIDEO01 = 1;
    public static final int GUMBALL = 2;
    public static final int MEMORY = 3;
    public static final int JETPACK = 4;
    public static final int VIDEO15 = 5;
    public static final int ROCKET = 6;
    public static final int DANCER = 7;
    public static final int SNOWDOWN = 8;
    public static final int VIDEO23 = 9;
    public static final int NUM_PINS = 10;

    private final Typeface mFont;

    private AbstractLaunch[] mAllLaunchers = new AbstractLaunch[NUM_PINS];
    private AbstractLaunch[] mLaunchers = new AbstractLaunch[NUM_PINS];

    public CardAdapter(SantaContext santaContext) {
        mAllLaunchers[SANTA] = new LaunchSanta(santaContext, this);
        mAllLaunchers[VIDEO01] = new LaunchVideo(santaContext, this,
                R.drawable.android_game_cards_santas_back, 1);
        mAllLaunchers[GUMBALL] = new LaunchGumball(santaContext, this);
        mAllLaunchers[MEMORY] = new LaunchMemory(santaContext, this);
        mAllLaunchers[JETPACK] = new LaunchJetpack(santaContext, this);
        mAllLaunchers[VIDEO15] = new LaunchVideo(santaContext, this,
                R.drawable.android_game_cards_office_prank, 1);
        mAllLaunchers[ROCKET] = new LaunchRocket(santaContext, this);
        mAllLaunchers[DANCER] = new LaunchDancer(santaContext, this);
        mAllLaunchers[SNOWDOWN] = new LaunchSnowdown(santaContext, this);
        mAllLaunchers[VIDEO23] = new LaunchVideo(santaContext, this,
                R.drawable.android_game_cards_elf_car, 23);

        updateMarkerVisibility();

        mFont = Typeface.createFromAsset(santaContext.getActivityContext().getAssets(),
                "Lobster-Regular.otf");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.layout_village_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setLauncher(mLaunchers[position], mFont);
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

        public void setLauncher(AbstractLaunch launcher, Typeface tf) {
            this.launcher = launcher;

            // Loading all of these beautiful images at full res is laggy without using
            // Glide, however this makes it asynchronous.  We should consider either compromising
            // on image resolution or doing some sort of nifty placeholder.
            Glide.with(itemView.getContext())
                    .fromResource()
                    .centerCrop()
                    .load(launcher.getCardResource())
                    .into(backgroundImageView);

            nameView.setText(launcher.getContentDescription());
            verbView.setText(launcher.getVerb());
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                verbView.setTypeface(tf);
            } else {
                verbView.setTypeface(tf, Typeface.ITALIC);
            }
            itemView.setContentDescription(launcher.getContentDescription());

            launcher.attachToView(this.itemView);
        }
    }
}
