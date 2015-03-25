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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.Toast;

/**
 * Partial implementation of a launch from the main village into a specific game or activity. All
 * launches are visually illustrated with a marker and must be defined with color, a badge and some
 * state that defines whether the marker is locked or available.
 */
public abstract class AbstractLaunch implements View.OnClickListener,
        View.OnLongClickListener, VoiceAction.VoiceActionHandler {

    public static final int STATE_LOCKED = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_FINISHED = 3;
    public static final int STATE_HIDDEN = 4;

    private static final String TAG = "AbstractLaunch";

    protected int mState = STATE_DISABLED;
    protected StartupActivity.SantaContext mContext;
    protected String mContentDescription;
    protected MarkerView mMarker;

    // Marker-specific style properties
    protected int mBadgeDrawable;
    protected int mMarkerColor;
    protected int mBadgePaddingLeft;
    protected int mBadgePaddingTop;
    protected int mBadgePaddingRight;

    /**
     * Constructs a new launch (marker).
     *
     * @param context              The application (Santa) context
     * @param contentDescriptionId The name of the marker, used for accessibility
     * @param badgeDrawable        The resource ID of the badge to draw on the marker
     * @param markerColor          The resource ID of the color in which to fill the marker
     * @param badgePaddingLeft     The left-side padding of the marker badge from the edge of the
     *                             marker
     * @param badgePaddingTop      The top-side padding of the marker badge from the edge of the
     *                             marker
     * @param badgePaddingRight    The right-side padding of the marker badge from the edge of the
     *                             marker
     * @see com.google.android.apps.santatracker.launch.MarkerView#setBadgePadding(int, int, int)
     */
    public AbstractLaunch(StartupActivity.SantaContext context, int contentDescriptionId,
            int badgeDrawable, int markerColor, int badgePaddingLeft,
            int badgePaddingTop, int badgePaddingRight) {
        initialise(context, contentDescriptionId, badgeDrawable, markerColor, badgePaddingLeft,
                badgePaddingTop, badgePaddingRight);
    }

    protected void initialise(StartupActivity.SantaContext context, int contentDescriptionId,
            int badgeDrawable, int markerColor, int badgePaddingLeft,
            int badgePaddingTop, int badgePaddingRight) {
        setContext(context);
        mState = STATE_DISABLED;
        mContentDescription = mContext.getResources().getString(contentDescriptionId);
        mBadgeDrawable = badgeDrawable;
        mMarkerColor = markerColor;
        mBadgePaddingLeft = badgePaddingLeft;
        mBadgePaddingTop = badgePaddingTop;
        mBadgePaddingRight = badgePaddingRight;
    }

    /** Sets the SantaContext. */
    public void setContext(StartupActivity.SantaContext context) {
        mContext = context;
        mState = STATE_DISABLED;
    }

    /** Retrieves the underlying image of the marker. */
    public MarkerView getImage() {
        return mMarker;
    }

    /**
     * Binds the image to the marker and sets up the visual properties. Used when the RecyclerView
     * wishes to draw the marker.
     */
    public void attachToView(MarkerView image) {
        mMarker = image;

        Resources r = mContext.getContext().getResources();
        image.setDrawable(r.getDrawable(mBadgeDrawable));
        image.setColor(r.getColor(mMarkerColor));
        image.setBadgePadding(r.getDimensionPixelOffset(mBadgePaddingLeft),
                r.getDimensionPixelOffset(mBadgePaddingTop),
                r.getDimensionPixelOffset(mBadgePaddingRight));
    }

    /** Recycles the image when not on screen. */
    public void detachFromView() {
        mMarker = null;
    }

    /** Attaches events to the marker, updating the image based on the current state. */
    public void applyState() {
        if (mMarker != null) {
            mMarker.setOnClickListener(this);
            mMarker.setOnLongClickListener(this);
            mMarker.setContentDescription(mContentDescription);
            setImage(mState);
        }
    }

    /**
     * Updates the state of the image (e.g. locked).
     *
     * @param state One of {@code STATE_LOCKED, STATE_READY, STATE_DISABLED, STATE_FINISHED,
     *              STATE_HIDDEN}
     */
    public void setState(int state) {
        if (mState != state) {
            mState = state;
            applyState();
        }
    }

    private void setImage(int state) {
        mMarker.setVisibility(View.VISIBLE);
        if (state == STATE_HIDDEN) {
            // special case, only hide the image
            mMarker.setVisibility(View.GONE);
        } else {
            mMarker.setLocked(false);
            mMarker.setDisabled(false);
            if (state == STATE_FINISHED || state == STATE_DISABLED) {
                mMarker.setDisabled(true);
            } else if (state == STATE_LOCKED) {
                mMarker.setLocked(true);
            }
            mMarker.setVisibility(View.VISIBLE);
            mMarker.invalidate();
        }
    }

    /**
     * Display a {@link android.widget.Toast} with a given string resource message.
     */
    protected void notify(Context context, int stringId) {
        Toast.makeText(context, context.getResources().getText(stringId), Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * Display a {@link android.widget.Toast} with a given string resource message with additional
     * string parameters.
     *
     * @see android.content.res.Resources#getString(int, Object...)
     */
    protected void notify(Context context, int stringId, Object... args) {
        Toast.makeText(context, context.getResources().getString(stringId, args),
                Toast.LENGTH_SHORT)
                .show();
    }

    /** Retrieves the current marker state. */
    public int getState() {
        return mState;
    }


    /**
     * Handles a voice action. Override if the implementation wishes to handle voice actions and
     * return true if it has been or will be handled.
     *
     * @param intent Google Voice Actions intent.
     * @return true if the action was handled or will be handled
     */
    @Override
    public boolean handleVoiceAction(Intent intent) {
        return false;
    }

    /**
     * Determines if this marker is a game. Default is true, so override is only necessary if
     * implementation is not a game.
     *
     * @return true if this launcher will launch a game, false otherwise.
     */
    public boolean isGame() {
        return true;
    }
}
