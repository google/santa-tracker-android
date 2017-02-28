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

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Toast;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.SantaLog;

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
    protected SantaContext mContext;
    protected String mContentDescription;
    protected int mCardDrawable;
    protected View mClickTarget;
    private LauncherDataChangedCallback mLauncherCallback;
    private View mLockedView;

    /**
     * Constructs a new launch (marker).
     *
     * @param context              The application (Santa) context
     * @param contentDescriptionId The name of the marker, used for accessibility
     * @param cardDrawable         The resource ID of the card to draw
     */
    public AbstractLaunch(SantaContext context, LauncherDataChangedCallback adapter, int contentDescriptionId,
            int cardDrawable) {
        initialise(context, adapter, contentDescriptionId, cardDrawable);
    }

    protected void initialise(SantaContext context, LauncherDataChangedCallback adapter, int contentDescriptionId,
            int cardDrawable) {
        setContext(context);
        mLauncherCallback = adapter;
        mState = STATE_DISABLED;
        mContentDescription = mContext.getResources().getString(contentDescriptionId);
        mCardDrawable = cardDrawable;
    }

    /** Sets the SantaContext. */
    public void setContext(SantaContext context) {
        mContext = context;
        mState = STATE_DISABLED;
    }

    public void attachToView(View view) {
        mClickTarget = view;
        mClickTarget.setOnClickListener(this);
    }

    public int getCardResource() {
        return mCardDrawable;
    }

    public View getClickTarget() {
        return mClickTarget;
    }

    public String getContentDescription() {
        return mContentDescription;
    }

    /** Attaches events to the marker, updating the image based on the current state. */
    public void applyState() {
        if (mLockedView != null) {
            if (mState == STATE_DISABLED || mState == STATE_LOCKED || mState == STATE_FINISHED) {
                mLockedView.setVisibility(View.VISIBLE);
            } else {
                mLockedView.setVisibility(View.GONE);
            }
        }
    }

    public void setLockedView(View lockedView) {
        mLockedView = lockedView;
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
            mLauncherCallback.refreshData();
        }
    }

    /**
     * Display a {@link android.widget.Toast} with a given string resource message.
     */
    protected void notify(Context context, int stringId) {
        notify(context, context.getResources().getText(stringId).toString());
    }

    /**
     * Display a {@link android.widget.Toast} with a given string resource message with additional
     * string parameters.
     *
     * @see android.content.res.Resources#getString(int, Object...)
     */
    protected void notify(Context context, int stringId, Object... args) {
        notify(context, context.getResources().getString(stringId, args));
    }

    /**
     * Display a {@link android.widget.Toast} with a given string.
     */
    protected void notify(Context context, String string) {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }

    /** Retrieves the current marker state. */
    public int getState() {
        return mState;
    }

    /**
     * Convenience method used for launching games.
     * Simulate onClick event if the intent received is of the type specified by
     * actionName and the extra value matches the content description of the current
     * instance.
     *
     * @param intent     the intent to examine
     * @param actionName the action name, for example VoiceAction.ACTION_PLAY_GAME
     * @param extraName  the extra name, for example VoiceAction.ACTION_PLAY_GAME_EXTRA
     * @return true if matched and OnClick was invoked
     */
    protected boolean clickIfMatchesDescription(Intent intent, String actionName,
            String extraName) {
        String action = intent.getAction();
        if (actionName.equals(action)) {
            String description = intent.getStringExtra(extraName);
            if (mContentDescription.equalsIgnoreCase(description)) {
                SantaLog.d(TAG,
                        String.format("Voice command: [%s] [%s]", actionName,
                                description));

                onClick(mClickTarget);
                return true;
            }
        }
        return false;
    }

    public String getVerb() {
        return mContext.getResources().getString(R.string.play);
    }

    /**
     * Handles a voice action. Override if the implementation wishes to handle voice actions and
     * return true if it has been or will be handled.
     *
     * @param intent Google Now Actions intent.
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

    /** Checck whether the app is running on Tv or not. */
    protected boolean isTV() {

        final Context context = mContext.getApplicationContext();
        UiModeManager manager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        return manager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    /**
     * Get a string message explaining that the game is unavailable.
     */
    protected String getDisabledString(@StringRes int gameNameId) {
        String gameName = mContext.getResources().getString(gameNameId);
        return mContext.getResources().getString(R.string.generic_game_disabled, gameName);
    }
}
