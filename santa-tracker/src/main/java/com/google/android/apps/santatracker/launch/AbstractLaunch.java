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

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityOptionsCompat;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.SantaLog;

/**
 * Partial implementation of a launch from the main village into a specific game or activity. All
 * launches are visually illustrated with a marker and must be defined with color, a badge and some
 * state that defines whether the marker is locked or available.
 */
public abstract class AbstractLaunch
        implements View.OnClickListener,
                View.OnLongClickListener,
                VoiceAction.VoiceActionHandler,
                Comparable<AbstractLaunch> {

    public static final int STATE_READY = 0;
    public static final int STATE_FINISHED = 1;
    public static final int STATE_LOCKED = 2;
    public static final int STATE_DISABLED = 3;
    public static final int STATE_HIDDEN = 4;

    private static final String TAG = "AbstractLaunch";

    private int mState = STATE_DISABLED;
    private boolean mIsFeatured = false;
    protected final SantaContext mContext;
    private String mTitle;
    private int mTitleRes;
    private String mCardImageUrl;
    private int mCardDrawableRes;
    private View mClickTarget;
    private LauncherDataChangedCallback mLauncherCallback;
    private View mLockedView;
    private ImageView mImageView;

    /**
     * Constructs a new launch (marker).
     *
     * @param context The application (Santa) context
     */
    public AbstractLaunch(SantaContext context, LauncherDataChangedCallback adapter) {
        this(context, adapter, 0, 0);
    }

    /**
     * Constructs a new launch (marker).
     *
     * @param context The application (Santa) context
     * @param contentDescriptionId The name of the marker, used for accessibility
     * @param cardDrawable The resource ID of the drawable for the card and splash screen.
     */
    public AbstractLaunch(
            SantaContext context,
            LauncherDataChangedCallback adapter,
            int contentDescriptionId,
            int cardDrawable) {
        mContext = context;
        mLauncherCallback = adapter;
        setState(STATE_DISABLED);
        mTitleRes = contentDescriptionId;
        mTitle =
                contentDescriptionId != 0
                        ? mContext.getResources().getString(contentDescriptionId)
                        : null;
        mCardDrawableRes = cardDrawable;
    }

    @Override
    public int compareTo(@NonNull AbstractLaunch launch) {
        if (this instanceof LaunchHeader) {
            // this is a header. Should be first on the list.
            return -1;
        } else if (launch instanceof LaunchHeader) {
            return 1;
        }
        if ((this.isFeatured() && launch.isFeatured())
                || (!this.isFeatured() && !launch.isFeatured())) {
            // If they're both featured, the state determines the order.
            return this.getState() - launch.getState();
        }
        if (this.isFeatured() && !launch.isFeatured()) {
            // The feature card trumps the not featured card.
            return -1;
        } else if (!this.isFeatured() && launch.isFeatured()) {
            // The feature card trumps the not featured card.
            return 1;
        }
        return this.getState() - launch.getState();
    }

    public boolean isReady() {
        return getState() == STATE_READY;
    }

    public boolean isFeatured() {
        return mIsFeatured;
    }

    public void setImageView(ImageView imageView) {
        mImageView = imageView;
    }

    ActivityOptionsCompat getActivityOptions() {
        ImageView imageView = getImageView();
        if (imageView != null) {
            return ActivityOptionsCompat.makeSceneTransitionAnimation(
                    mContext.getActivity(), imageView, imageView.getTransitionName());
        } else {
            // No transition animation on Android TV
            return ActivityOptionsCompat.makeBasic();
        }
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void attachToView(View view) {
        mClickTarget = view;
        mClickTarget.setOnClickListener(this);
    }

    View getClickTarget() {
        return mClickTarget;
    }

    public String getTitle() {
        return mTitle;
    }

    @DrawableRes
    public int getCardDrawableRes() {
        return mCardDrawableRes;
    }

    void setCardDrawableRes(@DrawableRes int drawableRes) {
        mCardDrawableRes = drawableRes;
    }

    public String getCardImageUrl() {
        return mCardImageUrl;
    }

    public void setCardImageUrl(String cardImageUrl) {
        mCardImageUrl = cardImageUrl;
    }

    @StringRes
    public int getTitleRes() {
        return mTitleRes;
    }

    /** Attaches events to the marker, updating the image based on the current state. */
    public void applyState() {
        if (mLockedView != null) {
            if (isLocked()) {
                mLockedView.setVisibility(View.VISIBLE);
            } else {
                mLockedView.setVisibility(View.GONE);
            }
        }
    }

    public boolean isLocked() {
        return getState() == STATE_DISABLED
                || getState() == STATE_LOCKED
                || getState() == STATE_FINISHED;
    }

    public void setLockedView(View lockedView) {
        mLockedView = lockedView;
    }

    /** Display a {@link android.widget.Toast} with a given string resource message. */
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

    /** Display a {@link android.widget.Toast} with a given string. */
    protected void notify(Context context, String string) {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }

    /** Retrieves the current marker state. */
    int getState() {
        return mState;
    }

    /**
     * Updates the state of the image (e.g. locked).
     *
     * @param state One of {@code STATE_LOCKED, STATE_READY, STATE_DISABLED, STATE_FINISHED,
     *     STATE_HIDDEN}
     */
    public void setState(boolean isFeatured, int state) {
        if (mState != state || mIsFeatured != isFeatured) {
            mState = state;
            mIsFeatured = isFeatured;
            applyState();
            mLauncherCallback.refreshData();
        }
    }

    void setState(int state) {
        if (mState != state) {
            mState = state;
            applyState();
            mLauncherCallback.refreshData();
        }
    }

    /**
     * Convenience method used for launching games. Simulate onClick event if the intent received is
     * of the type specified by actionName and the extra value matches the content description of
     * the current instance.
     *
     * @param intent the intent to examine
     * @param actionName the action name, for example VoiceAction.ACTION_PLAY_GAME
     * @param extraName the extra name, for example VoiceAction.ACTION_PLAY_GAME_EXTRA
     * @return true if matched and OnClick was invoked
     */
    boolean clickIfMatchesDescription(Intent intent, String actionName, String extraName) {
        String action = intent.getAction();
        if (actionName.equals(action)) {
            String description = intent.getStringExtra(extraName);
            if (mTitle.equalsIgnoreCase(description)) {
                SantaLog.d(TAG, String.format("Voice command: [%s] [%s]", actionName, description));

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

    /** Check whether the app is running on Tv or not. */
    protected boolean isTV() {

        final Context context = mContext.getApplicationContext();
        UiModeManager manager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        return manager != null
                && manager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    /** Get a string message explaining that the game is unavailable. */
    String getDisabledString(@StringRes int gameNameId) {
        String gameName = mContext.getResources().getString(gameNameId);
        return mContext.getResources().getString(R.string.generic_game_disabled, gameName);
    }

    /** Get a string message explaining that the game is still locked and not yet available. */
    String getLockedString(@StringRes int gameNameId) {
        String gameName = mContext.getResources().getString(gameNameId);
        return mContext.getResources().getString(R.string.generic_game_locked, gameName);
    }

    public int getLockedViewResource() {
        return -1;
    }
}
