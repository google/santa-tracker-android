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

import android.content.ComponentName;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

/** Holds common complication functionality for both Santa and Elf */
public abstract class AbstractBaseWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "BaseWatchFace";

    private static final int RIGHT_COMPLICATION_ID = 0;
    private static final int LOWER_COMPLICATION_ID = 1;
    private static final int LEFT_COMPLICATION_ID = 2;

    private static final int[] COMPLICATION_IDS = {
        RIGHT_COMPLICATION_ID, LOWER_COMPLICATION_ID, LEFT_COMPLICATION_ID
    };

    // Supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_LONG_TEXT,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SMALL_IMAGE,
            ComplicationData.TYPE_NO_PERMISSION
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        }
    };

    // Used by {@link ComplicationConfigActivity} to retrieve id for complication locations and
    // to check if complication location is supported.
    static int getComplicationId(
            AbstractComplicationConfigActivity.ComplicationLocation complicationLocation) {
        // Add any other supported locations here you would like to support. In our case, we are
        // only supporting a left and right complication.
        switch (complicationLocation) {
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            case LOWER:
                return LOWER_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link ComplicationConfigActivity} to retrieve all complication ids.
    static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ComplicationConfigActivity} to retrieve complication types supported by
    // location.
    static int[] getSupportedComplicationTypes(
            AbstractComplicationConfigActivity.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case LOWER:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[2];

            default:
                return new int[] {};
        }
    }

    /** Performs common complication actions. */
    protected abstract class Engine extends CanvasWatchFaceService.Engine {

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

        private boolean mAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                initializeComplications();
            }
        }

        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            assert leftComplicationDrawable != null;
            leftComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            assert rightComplicationDrawable != null;
            rightComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable lowerComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            assert lowerComplicationDrawable != null;
            lowerComplicationDrawable.setContext(getApplicationContext());

            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(LOWER_COMPLICATION_ID, lowerComplicationDrawable);

            setActiveComplications(COMPLICATION_IDS);

            // Sets default complication data providers to showcase complications before user
            // chooses. In this example we demonstrates three different types of complications

            // Type 1. Safe system default - the watch face does not need permission to access this
            // data and it's included in the system so no need to implement a complication data
            // provider. Easiest to implement
            setDefaultSystemComplicationProvider(
                    LEFT_COMPLICATION_ID,
                    SystemProviders.WATCH_BATTERY,
                    ComplicationData.TYPE_RANGED_VALUE);

            // Type 2. App's own complication data provider - in this case, the complication
            // countdown to Christmas from ChristmasCountdownProviderService.
            setDefaultComplicationProvider(
                    RIGHT_COMPLICATION_ID,
                    new ComponentName(
                            getApplicationContext(), ChristmasCountdownProviderService.class),
                    ComplicationData.TYPE_SHORT_TEXT);

            // Type 3. Unsafe system default - the watch face will need the user to grant permission
            // to the watch face before it can display anything. The drawable will show an
            // exclamation mark and when the user tap on it, it should show a permission dialog
            // see onTapCommand method in this class for details on how to handle this.
            setDefaultSystemComplicationProvider(
                    LOWER_COMPLICATION_ID,
                    SystemProviders.NEXT_EVENT,
                    ComplicationData.TYPE_LONG_TEXT);
        }

        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

                mAmbient = inAmbientMode;

                // Update drawable complications' ambient state.
                // Note: ComplicationDrawable handles switching between active/ambient colors, we
                // just have to inform it to enter ambient mode.
                ComplicationDrawable complicationDrawable;

                for (int complicationId : COMPLICATION_IDS) {
                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                    complicationDrawable.setInAmbientMode(mAmbient);
                }
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

                int sizeOfComplication = width / 4;
                int midpointOfScreen = width / 2;

                int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
                int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

                Rect leftBounds =
                        // Left, Top, Right, Bottom
                        new Rect(
                                horizontalOffset,
                                verticalOffset,
                                (horizontalOffset + sizeOfComplication),
                                (verticalOffset + sizeOfComplication));

                ComplicationDrawable leftComplicationDrawable =
                        mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
                leftComplicationDrawable.setBounds(leftBounds);

                Rect rightBounds =
                        // Left, Top, Right, Bottom
                        new Rect(
                                (midpointOfScreen + horizontalOffset),
                                verticalOffset,
                                (midpointOfScreen + horizontalOffset + sizeOfComplication),
                                (verticalOffset + sizeOfComplication));

                ComplicationDrawable rightComplicationDrawable =
                        mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
                rightComplicationDrawable.setBounds(rightBounds);

                Rect lowerBounds =
                        // Left, Top, Right, Bottom
                        new Rect(
                                (horizontalOffset + sizeOfComplication / 2),
                                (verticalOffset + sizeOfComplication),
                                (midpointOfScreen + horizontalOffset + sizeOfComplication / 2),
                                (verticalOffset + sizeOfComplication * 2));

                ComplicationDrawable lowerComplicationDrawable =
                        mComplicationDrawableSparseArray.get(LOWER_COMPLICATION_ID);
                lowerComplicationDrawable.setBounds(lowerBounds);
            }
        }

        void drawComplications(Canvas canvas, long currentTimeMillis) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

                ComplicationDrawable complicationDrawable;

                for (int complicationId : COMPLICATION_IDS) {
                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                    complicationDrawable.draw(canvas, currentTimeMillis);
                }
            }
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

                ComplicationData complicationData;
                ComplicationDrawable complicationDrawable;

                long currentTimeMillis = System.currentTimeMillis();

                for (int complicationId : COMPLICATION_IDS) {
                    complicationData = mActiveComplicationDataSparseArray.get(complicationId);

                    if ((complicationData != null)
                            && (complicationData.isActive(currentTimeMillis))
                            && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                            && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                        complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                        if (null != complicationDrawable) {
                            // See if the user has tap on a drawable that has an attached method
                            if (complicationDrawable.onTap(x, y, 0)) {
                                // Yes - the tapped complication has an intent. ComplicationDrawable
                                // has taken care of it.
                                break;
                            } else if (complicationDrawable.getBounds().contains(x, y)
                                    && complicationData.getType()
                                            == ComplicationData.TYPE_NO_PERMISSION) {
                                // The watch face does not have permission to receive data from the
                                // complication data provider and the user has just tapped on it.
                                // This can happen when the watch face has default complication
                                // providers which are "unsafe" (i.e. it requires user permission
                                // before data is send to the watch face) - one example is the next
                                // event complication data provider.
                                displayPermissionScreen();
                            }
                        }
                    }
                }
            }
        }
    }

    protected abstract Class<?> getWatchFaceService();

    private void displayPermissionScreen() {
        startActivity(
                ComplicationHelperActivity.createPermissionRequestHelperIntent(
                        this, new ComponentName(this, getWatchFaceService())));
    }
}
