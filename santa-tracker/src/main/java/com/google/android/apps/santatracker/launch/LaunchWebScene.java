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

import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import com.google.android.apps.santatracker.games.SplashActivity;
import com.google.android.apps.santatracker.web.WebSceneActivity;

/**
 * Launcher to launch web scnes. This requires Android M or above. By default, a web scene is hidden
 * until {@link #setData(boolean, boolean, boolean, String)} is called with a URL.
 *
 * @see WebSceneActivity
 */
public class LaunchWebScene extends AbstractLaunch {

    private String mWebUrl;
    private boolean mIsLandscape;
    @StringRes private int mName;
    @ColorRes private int mSplashScreenColorId;

    /**
     * Constructs a new web scene launcher.
     *
     * @param context The application (Santa) context
     * @param name Resource of name of this scene
     * @param cardDrawable Resource of the drawable that is shown in the launcher
     * @param splashScreenColorId Resource ID for the background color for the splash screen
     * @param adapter Callback for events
     */
    public LaunchWebScene(
            SantaContext context,
            LauncherDataChangedCallback adapter,
            @StringRes int name,
            @StringRes int contentDescriptionId,
            @DrawableRes int cardDrawable,
            @ColorRes int splashScreenColorId) {
        super(context, adapter, contentDescriptionId, cardDrawable);
        mName = name;
        setState(STATE_HIDDEN);
        mSplashScreenColorId = splashScreenColorId;
    }

    /**
     * Set the state of this launcher by providing a URL, landscape flag and the state. If
     * isDisabled is set, the scene is set to {@link #STATE_HIDDEN}. If isDisabled is not set and
     * the url is empty, the state is set to {@link #STATE_LOCKED}. Otherwise, the state is set to
     * {@link #STATE_READY} and the scene is enabled with the url and isLandscape parameter.
     *
     * @param isDisabled If true, this scene is disabled and is hidden.
     * @param isLandscape If true, the scene will be launched in landscape, otherwise in portrait.
     * @param url The URL to open
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void setData(
            boolean isFeatured,
            boolean isDisabled,
            boolean isLandscape,
            String url,
            String cardImageUrl) {
        int state;
        if (isDisabled) {
            state = STATE_HIDDEN;
        } else if (TextUtils.isEmpty(url)) {
            // Lock the scene if the url is not set.
            state = STATE_LOCKED;
        } else {
            state = STATE_READY;
        }
        setState(isFeatured, state);
        mWebUrl = url;
        mIsLandscape = isLandscape;
        setCardImageUrl(cardImageUrl);
    }

    public String getWebUrl() {
        return mWebUrl;
    }

    public boolean getIsLandscape() {
        return mIsLandscape;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (getState()) {
            case STATE_READY:
            case STATE_FINISHED:
                Intent launchIntent =
                        WebSceneActivity.Companion.intent(
                                mContext.getActivityContext(), getIsLandscape(), getWebUrl());
                Intent intent =
                        SplashActivity.getIntent(
                                mContext.getActivity(),
                                getCardDrawableRes(),
                                mName,
                                mSplashScreenColorId,
                                mIsLandscape,
                                getTitle(),
                                getImageView(),
                                launchIntent,
                                getCardImageUrl());
                mContext.launchActivity(intent, getActivityOptions());
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), getDisabledString(mName));
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), getLockedString(mName));
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (getState()) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getApplicationContext(), mName);
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), getDisabledString(mName));
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), getDisabledString(mName));
                break;
        }
        return true;
    }

    @Override
    public boolean handleVoiceAction(Intent intent) {
        return clickIfMatchesDescription(
                intent, VoiceAction.ACTION_PLAY_GAME, VoiceAction.ACTION_PLAY_GAME_EXTRA);
    }
}
