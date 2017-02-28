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

package com.google.android.apps.santatracker.map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.AllDestinationCursorLoader;
import com.google.android.apps.santatracker.data.Destination;
import com.google.android.apps.santatracker.data.DestinationCursor;
import com.google.android.apps.santatracker.data.PresentCounter;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.data.StreamCursor;
import com.google.android.apps.santatracker.data.StreamCursorLoader;
import com.google.android.apps.santatracker.data.StreamEntry;
import com.google.android.apps.santatracker.map.cardstream.CardAdapter;
import com.google.android.apps.santatracker.map.cardstream.DashboardFormats;
import com.google.android.apps.santatracker.map.cardstream.DashboardViewHolder;
import com.google.android.apps.santatracker.map.cardstream.TrackerCard;
import com.google.android.apps.santatracker.service.SantaService;
import com.google.android.apps.santatracker.service.SantaServiceMessages;
import com.google.android.apps.santatracker.util.AccessibilityUtil;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.Intents;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;


/**
 * Map Activity that shows Santa's destinations and his path on and after
 * Christmas.
 */
public class TvSantaMapActivity extends FragmentActivity implements
        SantaMapFragment.SantaMapInterface {

    private static String ARRIVING_IN, DEPARTING_IN, NO_NEXT_DESTINATION;

    // countdown update frequency (in ms)
    private static final int DESTINATION_COUNTDOWN_UPDATE_INTERVAL = 1000;

    // Percentage of presents to hand out when travelling between destinations
    // (the rest is handed out when the destination is reached)
    public static final double FACTOR_PRESENTS_TRAVELLING = 0.3;

    protected static final String TAG = "TvSantaMapActivity";

    private static final int LOADER_DESTINATIONS = 1;
    private static final int LOADER_STREAM = 2;

    private CountDownTimer mTimer;
    private PresentCounter mPresents = new PresentCounter();
    protected DestinationCursor mDestinations;

    // Fragments
    protected SantaMapFragment mMapFragment;

    // Activity State
    private boolean mHasDataLoaded = false;
    private boolean mIsLive = false;
    private boolean mResumed = false;
    private boolean mIgnoreNextUpdate = false;

    // Resource Strings
    private static String LOST_CONTACT_STRING, CURRENT_LOCATION, NEXT_LOCATION;

    private static String ANNOUNCE_TRAVEL_TO;
    private static String ANNOUNCE_ARRIVED_AT;

    // Server controlled data
    protected boolean mSwitchOff = true;
    protected long mOffset = 0L;
    protected long mFirstDeparture = 0L;
    protected long mFinalArrival = 0L;
    protected long mFinalDeparture = 0L;

    // Toggle when error accessing API and need to return to Village with error message when out of
    // locations
    private boolean mHaveApiError = false;

    // Service integration
    private Messenger mService = null;
    private boolean mIsBound = false;
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    // Stream
    private StreamEntry mNextStreamEntry = null;
    protected StreamCursor mStream;

    private CardAdapter mAdapter;

    private VerticalGridView mVerticalGridView;

    private AccessibilityManager mAccessibilityManager;

    private FirebaseAnalytics mMeasurement;
    private boolean mJumpingToUserDestination = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // App Measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mMeasurement, getString(R.string.analytics_screen_tracker));

        // [ANALYTICS SCREEN]: Tracker
        AnalyticsManager.sendScreenView(R.string.analytics_screen_tracker);

        setContentView(R.layout.activity_map_tv);

        mAccessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        Resources resources = getResources();

        LOST_CONTACT_STRING = resources.getString(R.string.lost_contact_with_santa);
        ANNOUNCE_ARRIVED_AT = resources.getString(R.string.santa_is_now_arriving_in_x);
        ARRIVING_IN = resources.getString(R.string.arriving_in);
        DEPARTING_IN = resources.getString(R.string.departing_in);
        NO_NEXT_DESTINATION = resources.getString(R.string.no_next_destination);
        CURRENT_LOCATION = resources.getString(R.string.current_location);
        NEXT_LOCATION = resources.getString(R.string.next_destination);

        // Concatenate String for 'travel to' announcement
        StringBuilder sb = new StringBuilder();
        sb.append(resources.getString(R.string.in_transit));
        sb.append(" ");
        sb.append(resources.getString(R.string.next_destination));
        sb.append(" %s");
        ANNOUNCE_TRAVEL_TO = sb.toString();
        sb.setLength(0);

        // Get Map fragments
        mMapFragment = (SantaMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_map);
        // Set Overscan Padding
        final int widthPadding
                = getResources().getDimensionPixelOffset(R.dimen.overscan_padding_width);
        final int heightPadding
                = getResources().getDimensionPixelOffset(R.dimen.overscan_padding_height);
        final int cardPadding = getResources().getDimensionPixelOffset(R.dimen.card_width);
        // Right side padding is twice as much to put padding on both sides of card.
        final int rightPadding = 2 * widthPadding + cardPadding;
        mMapFragment.setCamPadding(widthPadding, heightPadding, rightPadding, heightPadding);

        mVerticalGridView = (VerticalGridView) findViewById(R.id.stream);
        mVerticalGridView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
        mVerticalGridView.setHasFixedSize(false);

        mAdapter = new CardAdapter(
                getApplicationContext(), mCardAdapterListener, mDestinationListener, true);
        mAdapter.setHasStableIds(true);

        mVerticalGridView.setAdapter(mAdapter);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mResumed && hasFocus) {
            mMapFragment.resumeAudio();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        mVerticalGridView.requestFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, SantaService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // unregister and unbind from Services
        unregisterFromService();
    }

    @Override
    protected void onPause() {
        mResumed = false;

        // stop the countdown timer if running
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        // stop santa cam
        onSantacamStateChange(false);

        // Reset state
        mHasDataLoaded = false;
        mJumpingToUserDestination = false;
        mIsLive = false;
        mDestinations = null;
        mStream = null;

        super.onPause();
    }

    private void unregisterFromService() {
        if (mIsBound) {
            if (mService != null) {
                Message msg = Message
                        .obtain(null, SantaServiceMessages.MSG_SERVICE_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    // ignore if service is not available
                }
                mService = null;
            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_DESTINATIONS: {
                    return new AllDestinationCursorLoader(TvSantaMapActivity.this);
                }
                case LOADER_STREAM: {
                    return new StreamCursorLoader(getApplicationContext(), false);
                }
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            final int id = loader.getId();
            if (id == LOADER_DESTINATIONS) {
                // loader finished loading cursor, setup the helper
                mDestinations = new DestinationCursor(cursor);
                start();
            } else if (id == LOADER_STREAM) {
                mStream = new StreamCursor(cursor);
                addPastStream();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            switch (loader.getId()) {
                case LOADER_DESTINATIONS:
                    mDestinations = null;
                    break;
                case LOADER_STREAM:
                    mStream = null;
                    break;
            }
        }
    };

    /**
     * Finishes the current activity and starts the startup activity.
     */
    protected void returnToStartupActivity() {
        finish();
    }

    /**
     * Call when the map or destinations are ready. Checks if both are initialised and calls
     * startTracking if ready.
     */
    private void start() {
        // check that the cursor and map have been initialised
        if (mDestinations == null || !mMapFragment.isInitialised()) {
            return;
        }
        if (!mIsLive) {
            startTracking();
        }
    }

    /**
     * Moves the destination cursor from to the current destination and adds all visited locations
     * to the map.
     */
    protected void addVisitedLocations() {
        // add all visited destinations from the cursors current position to the map
        while (mDestinations.hasNext()
                && mDestinations.isInPast(SantaPreferences.getCurrentTime())) {
            Destination destination = mDestinations.getCurrent();
            mMapFragment.addLocation(destination);
            mAdapter.addDestination(false, destination, false);
            mDestinations.moveToNext();
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Displays a friendly toast and returns to the startup activity with the given message.
     */
    private void handleErrorFinish() {
        Log.d(TAG, "Lost contact, returning to village.");
        Toast.makeText(getApplicationContext(), LOST_CONTACT_STRING,
                Toast.LENGTH_LONG).show();
        returnToStartupActivity();
    }

    /**
     * Called when the map has been initialised and is ready to be used.
     */
    public void onMapInitialised() {
        // map initialised, start tracking
        start();
    }

    /**
     * Start tracking Santa. If Santa is already finished, return to the main launcher. All
     * destinations from the cursor's current position to the current time are added to the map and
     * the map is restored to its
     */
    protected void startTracking() {
        mIsLive = true;

        final long time = SantaPreferences.getCurrentTime();
        // Return to launch activity if Santa hasn't left yet or has already left for the next year
        if (time >= mFirstDeparture && time < mFinalArrival) {
            // It's Christmas and Santa is travelling
            startOnChristmas();
        } else {
            // Any other state, return back to Village
            returnToStartupActivity();
        }
    }

    private void startOnChristmas() {
        SantaLog.d(TAG, "start on christmas");
        addVisitedLocations();
        // Load the stream data once all past locations have been added, based on the last visited
        // location
        getSupportLoaderManager()
                .restartLoader(LOADER_STREAM, null, mLoaderCallbacks);
        // determine santa's status - visiting or travelling?
        if (!mDestinations.hasNext()) {
            // sanity check - already finished, no destinations left
            returnToStartupActivity();
        } else if (mDestinations.isVisiting(SantaPreferences.getCurrentTime())) {
            // currently visiting a location
            Destination d = mDestinations.getCurrent();
            // move santa marker
            visitDestination(d, false);
            setNextDestination(d);
            // enable santa cam and center on santa
            mMapFragment.enableSantaCam(true);
        } else {
            // not currently visiting a location, en route to next destination
            // enable santacam, but do not move camera - this is done
            // through a callback once the santa animation has started
            mMapFragment.enableSantaCam(true);
            // get the destination and animate santa
            Destination d = mDestinations.getCurrent();
            // animate to next destination
            // marker at origin has already been set above, does not need to be
            // added again.
            travelToDestination(null, d);
        }
    }

    /**
     * Call when Santa is en route to the given destination.
     */
    private void travelToDestination(final Destination origin,
                                     final Destination nextDestination) {

        if (origin != null) {
            // add marker at origin position to map.
            mMapFragment.addLocation(origin);
        }

        // check if finished
        if (mDestinations.isFinished() || nextDestination == null) {
            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_tracker),
                    getString(R.string.analytics_tracker_action_finished),
                    getString(R.string.analytics_tracker_error_nodata));

            // [ANALYTICS EVENT]: Error NoData after API error
            AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                    R.string.analytics_tracker_action_finished,
                    R.string.analytics_tracker_error_nodata);

            // No more destinations left, return to village
            returnToStartupActivity();
            return;
        }

        if (mHaveApiError) {
            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_tracker),
                    getString(R.string.analytics_tracker_action_error),
                    getString(R.string.analytics_tracker_error_nodata));

            // [ANALYTICS EVENT]: Error NoData after API error
            AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                    R.string.analytics_tracker_action_error,
                    R.string.analytics_tracker_error_nodata);
            handleErrorFinish();
            return;
        }

        final String nextString = DashboardFormats.formatDestination(nextDestination);
        setNextLocation(nextString);
        setNextDestination(nextDestination);
        setCurrentLocation(null);

        // get the previous position
        Destination previous = mDestinations.getPrevious();

        SantaLog.d(TAG, "Travel: " + (origin != null ? origin.identifier : "null") + " -> "
                + nextDestination.identifier +
                " prev=" + (previous != null ? previous.identifier : "null"));

        // if this is the very first location, move santa directly
        if (previous == null) {
            mMapFragment.setSantaVisiting(nextDestination, false);
            mPresents.init(0,
                    nextDestination.presentsDelivered, nextDestination.arrival,
                    nextDestination.departure);
        } else {
            mMapFragment.setSantaTravelling(previous, nextDestination, !mJumpingToUserDestination);
            // only hand out X% of presents during travel
            long presentsEnd = previous.presentsDelivered + Math
                    .round((nextDestination.presentsDeliveredAtDestination)
                            * FACTOR_PRESENTS_TRAVELLING);
            mPresents.init(previous.presentsDelivered,
                    presentsEnd, previous.departure,
                    nextDestination.arrival);
        }

        // Notify dashboard to send accessibility event
        AccessibilityUtil.announceText(String.format(ANNOUNCE_TRAVEL_TO, nextString),
                mVerticalGridView, mAccessibilityManager);

        // cancel the countdown if it is already running
        if (mTimer != null) {
            mTimer.cancel();
        }

        mTimer = new CountDownTimer(nextDestination.arrival - SantaPreferences.getCurrentTime(),
                DESTINATION_COUNTDOWN_UPDATE_INTERVAL) {

            @Override
            public void onTick(long millisUntilFinished) {
                countdownTick();
            }

            @Override
            public void onFinish() {
                // reached destination - visit destination
                visitDestination(nextDestination, true);
            }
        };
        if (mResumed) {
            mTimer.start();
        }
    }

    private DashboardViewHolder getDashboardViewHolder() {
        return (DashboardViewHolder) mVerticalGridView.findViewHolderForItemId(mAdapter.getDashboardId());
    }

    private void setCurrentLocation(String location) {
        final DashboardViewHolder holder = getDashboardViewHolder();
        if (holder == null) {
            return;
        }
        if (TextUtils.isEmpty(location)) {
            holder.countdownLabel.setText(ARRIVING_IN);
        } else {
            holder.countdownLabel.setText(DEPARTING_IN);
            holder.locationLabel.setText(CURRENT_LOCATION);
            holder.location.setText(location);
        }
    }

    private void setNextLocation(final String s) {
        final String nextLocation = s == null ? NO_NEXT_DESTINATION : s;
        mAdapter.setNextLocation(nextLocation);
        final DashboardViewHolder holder = getDashboardViewHolder();
        if (null == holder) {
            return;
        }
        holder.location.post(new Runnable() {
            @Override
            public void run() {
                holder.locationLabel.setText(NEXT_LOCATION);
                holder.location.setText(nextLocation);
            }
        });
    }

    private void setNextDestination(Destination next) {
        mAdapter.addDestination(false, next, false);
    }

    /**
     * Call when Santa is to visit a location.
     */
    private void visitDestination(final Destination destination, boolean playSound) {

        // Only visit this location if there is a following destination
        // Otherwise out of data or at North Pole
        if (mDestinations.isLast()) {
            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_tracker),
                    getString(R.string.analytics_tracker_action_error),
                    getString(R.string.analytics_tracker_error_nodata));

            // [ANALYTICS EVENT]: Error NoData
            AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                    R.string.analytics_tracker_action_error,
                    R.string.analytics_tracker_error_nodata);

            Toast.makeText(this, R.string.lost_contact_with_santa, Toast.LENGTH_LONG).show();
            returnToStartupActivity();
            return;
        }

        Destination nextDestination = mDestinations.getPeekNext();
        SantaLog.d(TAG, "Arrived: " + destination.identifier + " current=" + mDestinations
                .getCurrent().identifier + " next = " + nextDestination + " next id="
                + nextDestination);

        // hand out the remaining presents for this location, explicit to ensure counter is always
        // in correct state and does not depend on anything else at runtime.
        final long presentsStart = destination.presentsDelivered -
                destination.presentsDeliveredAtDestination +
                Math.round(
                        (destination.presentsDeliveredAtDestination)
                                * (1.0f - FACTOR_PRESENTS_TRAVELLING)
                );

        mPresents.init(presentsStart, destination.presentsDelivered,
                destination.arrival, destination.departure);

        final String destinationString = DashboardFormats.formatDestination(destination);
        setCurrentLocation(destinationString);

        mMapFragment.setSantaVisiting(destination, playSound);

        // Notify dashboard to send accessibility event
        AccessibilityUtil
                .announceText(String.format(ANNOUNCE_ARRIVED_AT, destination.getPrintName()),
                        mVerticalGridView, mAccessibilityManager);

        // cancel the countdown if it is already running
        if (mTimer != null) {
            mTimer.cancel();
        }

        // Count down until departure
        mTimer = new CountDownTimer(destination.departure
                - SantaPreferences.getCurrentTime(),
                DESTINATION_COUNTDOWN_UPDATE_INTERVAL) {

            @Override
            public void onTick(long millisUntilFinished) {
                countdownTick();
            }

            @Override
            public void onFinish() {
                // finished at this destination, move to the next one
                travelToDestination(mDestinations.getCurrent(),
                        mDestinations.getNext());
            }

        };
        if (mResumed) {
            mTimer.start();
        }
    }

    private void setDestinationPhotoDisabled(boolean disablePhoto) {
        mAdapter.setDestinationPhotoDisabled(disablePhoto);
    }

    private void setPresentsDelivered(final String presentsDelivered) {
        DashboardViewHolder holder = getDashboardViewHolder();
        if (holder == null) {
            return;
        }
        holder.presents.setText(presentsDelivered);
    }

    private void countdownTick() {
        final long presents = mPresents
                .getPresents(SantaPreferences.getCurrentTime());
        final String presentsString = DashboardFormats.formatPresents(presents);
        setPresentsDelivered(presentsString);

        // Check if next stream card should be displayed
        if (mNextStreamEntry != null && mStream != null &&
                SantaPreferences.getCurrentTime() >= mNextStreamEntry.timestamp) {
            addStreamEntry(mNextStreamEntry);
            mNextStreamEntry = mStream.getNext();
        }
    }

    private void addPastStream() {
        // add all visited destinations from the cursors current position to the map
        StreamEntry next = mStream.getCurrent();
        while (next != null && next.timestamp < SantaPreferences.getCurrentTime()) {
            addStreamEntry(mStream.getCurrent());
            next = mStream.getNext();
        }
        mNextStreamEntry = next;
    }

    private void addStreamEntry(StreamEntry entry) {
        SantaLog.d(TAG, "Add Stream entry: " + entry.timestamp);
        TrackerCard card = mAdapter.addStreamEntry(entry);
        announceNewCard(card);
    }

    private void announceNewCard(TrackerCard card) {
        if (mAccessibilityManager == null) {
            return;
        }
        String text = null;

        if (card instanceof TrackerCard.FactoidCard) {
            text = getString(R.string.new_trivia_from_santa);
        } else if (card instanceof TrackerCard.MovieCard) {
            text = getString(R.string.new_video_from_santa);
        } else if (card instanceof TrackerCard.PhotoCard) {
            text = getString(R.string.new_photo_from_santa);
        } else if (card instanceof TrackerCard.StatusCard) {
            text = getString(R.string.new_update_from_santa);
        }

        if (text != null) {
            // Announce the new card
            AccessibilityUtil.announceText(text, mVerticalGridView, mAccessibilityManager);
        }
    }

    @Override
    public void onShowDestination(Destination destination) {
        // Nothing to do on TV
    }

    @Override
    public void onClearDestination() {
        // Nothing to do on TV
    }

    /**
     * Called when the map is clicked
     */
    @Override
    public void mapClickAction() {
        // Nothing to do
    }

    @Override
    public void onSantacamStateChange(boolean santacamEnabled) {
        // Noting to do
    }

    private CardAdapter.DestinationCardKeyListener mDestinationListener
            = new CardAdapter.DestinationCardKeyListener() {
        @Override
        public void onJumpToDestination(LatLng destination) {

            if (mMapFragment == null || !mMapFragment.isInitialised()) {
                return;
            }

            if (!mJumpingToUserDestination) {
                Log.d(TAG, "onJumpToDestination:" + destination.toString());
                mJumpingToUserDestination = true;
                mMapFragment.jumpToDestination(destination);
            }
        }

        @Override
        public void onFinish() {

            if (mMapFragment == null || !mMapFragment.isInitialised()) {
                return;
            }

            mJumpingToUserDestination = false;
            Log.d(TAG, "onJumpToDestinationFinish.");
            mMapFragment.enableSantaCam(true);
        }

        @Override
        public boolean onMoveBy(KeyEvent event) {

            // TODO (chansuk) Allow a user to move around the map by using D-Pad
            return false;
        }
    };

    private CardAdapter.CardAdapterListener mCardAdapterListener
            = new CardAdapter.CardAdapterListener() {

        @Override
        public void onOpenStreetView(Destination.StreetView streetView) {
            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_tracker),
                    getString(R.string.analytics_tracker_action_streetview),
                    streetView.id);

            // [ANALYTICS EVENT]: StreetView
            AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                    R.string.analytics_tracker_action_streetview,
                    streetView.id);
            Intent intent = Intents.getStreetViewIntent(getString(R.string.streetview_uri), streetView);
            startActivity(intent);
        }

        @Override
        public void onPlayVideo(String youtubeId) {
            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_tracker),
                    getString(R.string.analytics_tracker_action_video),
                    youtubeId);

            // [ANALYTICS EVENT]: Video
            AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                    R.string.analytics_tracker_action_video,
                    youtubeId);
            Intent intent = Intents.getYoutubeIntent(mVerticalGridView.getContext(), youtubeId);
            startActivity(intent);
        }

    };

    private static class IncomingHandler extends Handler {

        private final WeakReference<TvSantaMapActivity> mActivityRef;

        public IncomingHandler(TvSantaMapActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SantaLog.d(TAG, "message=" + msg.what);
            final TvSantaMapActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }
            if (!activity.mIgnoreNextUpdate ||
                    msg.what == SantaServiceMessages.MSG_SERVICE_STATUS) {
                // ignore all updates while flag is toggled until status update is received
                switch (msg.what) {
                    case SantaServiceMessages.MSG_SERVICE_STATE_BEGIN:
                        // beginning full state update, ignore if already live
                        if (activity.mIsLive) {
                            activity.mIgnoreNextUpdate = true;
                        }
                        break;
                    case SantaServiceMessages.MSG_SERVICE_STATUS:
                        // Current state of service, received once when connecting, reset ignore
                        activity.mIgnoreNextUpdate = false;

                        switch (msg.arg1) {
                            case SantaServiceMessages.STATUS_IDLE:
                                activity.mHaveApiError = false;
                                if (!activity.mHasDataLoaded) {
                                    activity.mHasDataLoaded = true;
                                    activity.getSupportLoaderManager()
                                            .restartLoader(LOADER_DESTINATIONS, null,
                                                    activity.mLoaderCallbacks);
                                }
                                break;
                            case SantaServiceMessages.STATUS_ERROR_NODATA:
                            case SantaServiceMessages.STATUS_ERROR:
                                Log.d(TAG, "Santa tracking error 3, continue for now");
                                activity.mHaveApiError = true;
                                break;
                            case SantaServiceMessages.STATUS_PROCESSING:
                                // wait for success, but tell user we are waiting
                                Toast.makeText(activity, R.string.contacting_santa,
                                        Toast.LENGTH_LONG).show();
                                activity.mHaveApiError = false;
                                break;
                        }
                        break;
                    case SantaServiceMessages.MSG_INPROGRESS_UPDATE_ROUTE:
                        Log.d(TAG, "Santa tracking update 0 - returning.");
                        // route is about to be updated, return to StartupActivity
                        activity.handleErrorFinish();
                        break;
                    case SantaServiceMessages.MSG_UPDATED_STREAM:
                        // stream data has been updated - requery data
                        if (activity.mHasDataLoaded && activity.mStream != null) {
                            Log.d(TAG, "Santa stream update received.");
                            activity.getSupportLoaderManager().restartLoader(LOADER_STREAM, null,
                                    activity.mLoaderCallbacks);
                        }
                        break;
                    case SantaServiceMessages.MSG_UPDATED_ROUTE:
                        // route data has been updated - requery data
                        if (activity.mHasDataLoaded && activity.mDestinations != null) {
                            Log.d(TAG, "Santa tracking update 1 received.");
                            activity.getSupportLoaderManager().restartLoader(LOADER_DESTINATIONS,
                                    null, activity.mLoaderCallbacks);
                        }
                        break;
                    case SantaServiceMessages.MSG_UPDATED_ONOFF:
                        // exit if flag has been set
                        activity.mSwitchOff = (msg.arg1 == SantaServiceMessages.SWITCH_OFF);
                        if (activity.mSwitchOff) {
                            Log.d(TAG, "Lost Santa.");

                            if (mActivityRef.get() != null) {
                                // App Measurement
                                Context context = mActivityRef.get();
                                FirebaseAnalytics measurement = FirebaseAnalytics.getInstance(context);
                                MeasurementManager.recordCustomEvent(measurement,
                                        context.getString(R.string.analytics_event_category_tracker),
                                        context.getString(R.string.analytics_tracker_action_error),
                                        context.getString(R.string.analytics_tracker_error_switchoff));
                            }

                            // [ANALYTICS EVENT]: Error SwitchOff
                            AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                                    R.string.analytics_tracker_action_error,
                                    R.string.analytics_tracker_error_switchoff);
                            activity.handleErrorFinish();
                        }
                        break;
                    case SantaServiceMessages.MSG_UPDATED_TIMES:
                        onMessageUpdatedTimes(activity, msg);
                        break;
                    case SantaServiceMessages.MSG_UPDATED_DESTINATIONPHOTO:
                        final boolean disablePhoto = msg.arg1 == SantaServiceMessages.DISABLED;
                        activity.setDestinationPhotoDisabled(disablePhoto);
                        break;
                    case SantaServiceMessages.MSG_ERROR_NODATA:
                        //for no data: wait to run out of locations, proceed with normal error handling
                    case SantaServiceMessages.MSG_ERROR:
                        // Error accessing the API - ignore and run until out of locations
                        Log.d(TAG, "Couldn't track Santa, continue for now.");
                        activity.mHaveApiError = true;
                        break;
                    case SantaServiceMessages.MSG_SUCCESS:
                        activity.mHaveApiError = false;
                        // If data has been received for first time, start tracking
                        // Otherwise ignore all other updates
                        if (!activity.mHasDataLoaded) {
                            activity.mHasDataLoaded = true;
                            activity.getSupportLoaderManager().restartLoader(LOADER_DESTINATIONS,
                                    null, activity.mLoaderCallbacks);
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }

            }
        }

        private static boolean hasSignificantChange(long newOffset, TvSantaMapActivity activity) {
            return newOffset >
                    activity.mOffset + SantaPreferences.OFFSET_ACCEPTABLE_RANGE_DIFFERENCE ||
                    newOffset <
                            activity.mOffset - SantaPreferences.OFFSET_ACCEPTABLE_RANGE_DIFFERENCE;
        }

        private void onMessageUpdatedTimes(TvSantaMapActivity activity, Message msg) {
            Bundle b = (Bundle) msg.obj;
            long newOffset = b.getLong(SantaServiceMessages.BUNDLE_OFFSET);
            // If offset has changed significantly, return to village
            if (activity.mHasDataLoaded && hasSignificantChange(newOffset, activity)) {
                Log.d(TAG, "Santa tracking update 2 - returning.");

                if (mActivityRef.get() != null) {
                    // App Measurement
                    Context context = mActivityRef.get();
                    FirebaseAnalytics measurement = FirebaseAnalytics.getInstance(context);
                    MeasurementManager.recordCustomEvent(measurement,
                            context.getString(R.string.analytics_event_category_tracker),
                            context.getString(R.string.analytics_tracker_action_error),
                            context.getString(R.string.analytics_tracker_error_timeupdate));
                }

                // [ANALYTICS EVENT]: Error TimeUpdate
                AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                        R.string.analytics_tracker_action_error,
                        R.string.analytics_tracker_error_timeupdate);

                activity.handleErrorFinish();

            } else if (!activity.mHasDataLoaded && newOffset != activity.mOffset) {
                // New offset but data has not been loaded yet, cache new offset
                activity.mOffset = newOffset;
                SantaPreferences.cacheOffset(activity.mOffset);
            }

            activity.mFinalArrival = b.getLong(SantaServiceMessages.BUNDLE_FINAL_ARRIVAL);
            activity.mFinalDeparture = b.getLong(SantaServiceMessages.BUNDLE_FINAL_DEPARTURE);
            activity.mFirstDeparture = b.getLong(SantaServiceMessages.BUNDLE_FIRST_DEPARTURE);
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            mIsBound = true;

            //reply with local Messenger to establish bi-directional communication
            Message msg = Message.obtain(null, SantaServiceMessages.MSG_SERVICE_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                // Could not connect to Service, connection will be terminated soon.
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
        }
    };
}
