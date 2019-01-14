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
package com.google.android.apps.santatracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import java.util.concurrent.Executors;

/**
 * The watch-side config activity which allows for setting the left, right and bottom complications
 * of watch face.
 */
public abstract class AbstractComplicationConfigActivity extends Activity
        implements View.OnClickListener {

    private static final String TAG = "ConfigActivity";

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;

    /**
     * Used by associated watch face to let this configuration Activity know which complication
     * locations are supported, their ids, and supported complication data types.
     */
    public enum ComplicationLocation {
        RIGHT,
        LOWER,
        LEFT
    }

    private int mRightComplicationId;
    private int mLowerComplicationId;
    private int mLeftComplicationId;

    // Selected complication id by user.
    private int mSelectedComplicationId;

    // ComponentName used to identify a specific service that renders the watch face.
    private ComponentName mWatchFaceComponentName;

    // Required to retrieve complication data from watch face for preview.
    private ProviderInfoRetriever mProviderInfoRetriever;

    private ImageView mRightComplicationBackground;
    private ImageView mLowerComplicationBackground;
    private ImageView mLeftComplicationBackground;

    private ImageButton mRightComplication;
    private ImageButton mLowerComplication;
    private ImageButton mLeftComplication;

    private Drawable mDefaultAddComplicationDrawable;
    private Drawable mDottedBackground;
    private Drawable mSolidBackground;

    protected abstract int getBackgroundResource();

    protected abstract Class<?> getWatchFaceService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Complications is not supported by Android Wear 1.0, i.e. below Nougat MR1 (API 25)
        // Only display the complication config screen if it is >= API 25
        // Otherwise display a splash screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            setContentView(R.layout.activity_config);

            mDefaultAddComplicationDrawable = getDrawable(R.drawable.ic_add);
            mDottedBackground = getDrawable(R.drawable.dotted_circle_bg);
            mSolidBackground = getDrawable(R.drawable.solid_circle_bg);

            mSelectedComplicationId = -1;

            mLeftComplicationId =
                    AbstractBaseWatchFaceService.getComplicationId(ComplicationLocation.LEFT);
            mRightComplicationId =
                    AbstractBaseWatchFaceService.getComplicationId(ComplicationLocation.RIGHT);
            mLowerComplicationId =
                    AbstractBaseWatchFaceService.getComplicationId(ComplicationLocation.LOWER);

            mWatchFaceComponentName =
                    new ComponentName(getApplicationContext(), getWatchFaceService());

            // Sets up left complication preview.
            mLeftComplicationBackground = findViewById(R.id.left_complication_background);
            mLeftComplication = findViewById(R.id.left_complication);
            mLeftComplication.setOnClickListener(this);

            // Sets up right complication preview.
            mRightComplicationBackground = findViewById(R.id.right_complication_background);
            mRightComplication = findViewById(R.id.right_complication);
            mRightComplication.setOnClickListener(this);

            // Sets up lower complication preview.
            mLowerComplicationBackground = findViewById(R.id.lower_complication_background);
            mLowerComplication = findViewById(R.id.lower_complication);
            mLowerComplication.setOnClickListener(this);

            // Initialization of code to retrieve active complication data for the watch face.
            mProviderInfoRetriever =
                    new ProviderInfoRetriever(
                            getApplicationContext(), Executors.newCachedThreadPool());
            mProviderInfoRetriever.init();

            // Set the background to Santa or Elf as appropriate
            ImageView background = findViewById(R.id.watch_face_background);
            background.setBackgroundResource(getBackgroundResource());

            retrieveInitialComplicationsData();
        } else {
            setContentView(R.layout.activity_about_santa);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && null != mProviderInfoRetriever) {
            // Required to release retriever for active complication data.
            mProviderInfoRetriever.release();
        }
    }

    public void retrieveInitialComplicationsData() {

        final int[] complicationIds = AbstractBaseWatchFaceService.getComplicationIds();

        mProviderInfoRetriever.retrieveProviderInfo(
                new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                    @Override
                    public void onProviderInfoReceived(
                            int watchFaceComplicationId,
                            @Nullable ComplicationProviderInfo complicationProviderInfo) {

                        Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo);

                        updateComplicationViews(watchFaceComplicationId, complicationProviderInfo);
                    }
                },
                mWatchFaceComponentName,
                complicationIds);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mLeftComplication)) {
            Log.d(TAG, "Left Complication click()");
            launchComplicationHelperActivity(ComplicationLocation.LEFT);

        } else if (view.equals(mRightComplication)) {
            Log.d(TAG, "Right Complication click()");
            launchComplicationHelperActivity(ComplicationLocation.RIGHT);
        } else if (view.equals(mLowerComplication)) {
            Log.d(TAG, "Right Complication click()");
            launchComplicationHelperActivity(ComplicationLocation.LOWER);
        }
    }

    // Verifies the watch face supports the complication location, then launches the helper
    // class, so user can choose their complication data provider.
    private void launchComplicationHelperActivity(ComplicationLocation complicationLocation) {

        mSelectedComplicationId =
                AbstractBaseWatchFaceService.getComplicationId(complicationLocation);

        if (mSelectedComplicationId >= 0) {

            int[] supportedTypes =
                    AbstractBaseWatchFaceService.getSupportedComplicationTypes(
                            complicationLocation);

            startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            getApplicationContext(),
                            mWatchFaceComponentName,
                            mSelectedComplicationId,
                            supportedTypes),
                    AbstractComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);

        } else {
            Log.d(TAG, "Complication not supported by watch face.");
        }
    }

    private void updateComplicationViews(
            int watchFaceComplicationId, ComplicationProviderInfo complicationProviderInfo) {
        Log.d(TAG, "updateComplicationViews(): id: " + watchFaceComplicationId);
        Log.d(TAG, "\tinfo: " + complicationProviderInfo);

        if (watchFaceComplicationId == mLeftComplicationId) {
            toggleAddComplications(
                    mLeftComplication, mLeftComplicationBackground, complicationProviderInfo);
        } else if (watchFaceComplicationId == mRightComplicationId) {
            toggleAddComplications(
                    mRightComplication, mRightComplicationBackground, complicationProviderInfo);
        } else if (watchFaceComplicationId == mLowerComplicationId) {
            toggleAddComplications(
                    mLowerComplication, mLowerComplicationBackground, complicationProviderInfo);
        }
    }

    /**
     * Helps toggle the complication and complication background view when the complication is set
     * or not set.
     *
     * <p>If no complication data provider is set, the background will be dotted line circle and
     * complication is set to a cross.
     *
     * <p>If complication data provider is set, the background will be a solid line circle and
     * complication will be set to the icon of the provider.
     */
    private void toggleAddComplications(
            ImageView complication,
            ImageView complicationBackground,
            ComplicationProviderInfo complicationProviderInfo) {

        if (complicationProviderInfo != null) {
            complication.setImageIcon(complicationProviderInfo.providerIcon);
            complicationBackground.setImageDrawable(mSolidBackground);
        } else {
            complication.setImageDrawable(mDefaultAddComplicationDrawable);
            complicationBackground.setImageDrawable(mDottedBackground);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);

            if (mSelectedComplicationId >= 0) {
                updateComplicationViews(mSelectedComplicationId, complicationProviderInfo);
            }
        }
    }
}
