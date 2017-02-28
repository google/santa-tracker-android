/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.games.cityquiz;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.EndOfGameView;
import com.google.android.apps.santatracker.games.common.PlayGamesActivity;
import com.google.android.apps.santatracker.launch.StartupActivity;
import com.google.android.apps.santatracker.util.FontHelper;
import com.google.android.apps.santatracker.util.MapHelper;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.apps.santatracker.util.VectorUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;

import java.lang.ref.WeakReference;

/**
 * Main container for the City Quiz game.
 */
public class CityQuizActivity extends PlayGamesActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        ViewTreeObserver.OnGlobalLayoutListener {

    private static final String TAG = CityQuizActivity.class.getSimpleName();
    private static final int NEXT_ROUND_DELAY = 3500;
    public static final int CORRECT_MARKER = 0;
    public static final int FIRST_FAKE_MARKER = 1;
    public static final int SECOND_FAKE_MARKER = 2;
    public static final int IMAGE_LOAD_TIMEOUT_MILLIS = 5000; // 5 seconds
    public static final String CITY_QUIZ_ROUND_COUNT_CONFIG_KEY = "CityQuizRoundCount";

    private CityQuizGame mCityQuizGame;

    private ImageView mCityImageView;
    private ProgressBar mCityImageProgressBar;
    private ImageView mCloudOffImageView;
    private CityImageGlideListener mCityImageGlideListener;
    private TextView mCityImageAuthorTextView;
    private SupportMapFragment mSupportMapFragment;
    private GoogleMap mGoogleMap;
    private boolean mMapLaidOut;
    private boolean mMapReady;
    private boolean mInitialRoundLoaded;
    private TextView mRoundCountTextView;
    private TextView mPointsTextView;
    private View mMapScrim;

    private Handler mHandler;

    private FirebaseAnalytics mAnalytics;

    public CityQuizActivity() {
        super(R.layout.activity_city_quiz, StartupActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // [ANALYTICS]
        mAnalytics = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mAnalytics,
                getString(R.string.analytics_screen_city_quiz));

        mCityImageView = (ImageView) findViewById(R.id.cityQuizImageView);
        mCityImageProgressBar = (ProgressBar) findViewById(R.id.cityImageProgressBar);
        mCloudOffImageView = (ImageView) findViewById(R.id.cloudOffImageView);
        mCityImageAuthorTextView = (TextView) findViewById(R.id.cityImageAuthorTextView);
        mMapScrim = findViewById(R.id.map_scrim);

        mCityImageGlideListener = new CityImageGlideListener(this);

        // Clicking the "offline" image will attempt a reload
        mCloudOffImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CityQuizRound round = mCityQuizGame.getCurrentRound();
                if (round != null && round.getCity() != null) {
                    loadCityImage(round.getCity().getImageName(), round.getCity().getImageAuthor());
                }
            }
        });

        // Map scrim prevents clicking on map
        mMapScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // No-op, eat the click
            }
        });

        // Santa-fonts
        FontHelper.makeLobster((TextView) findViewById(R.id.title_city_quiz));
        FontHelper.makeLobster((TextView) findViewById(R.id.text_what_city));

        mSupportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mSupportMapFragment.getView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        mSupportMapFragment.getMapAsync(this);

        long roundCount = FirebaseRemoteConfig.getInstance().getLong(CITY_QUIZ_ROUND_COUNT_CONFIG_KEY);
        mCityQuizGame = new CityQuizGame(this, (int) roundCount);

        mRoundCountTextView = (TextView) findViewById(R.id.roundCountTextView);
        mPointsTextView = (TextView) findViewById(R.id.pointsTextView);

        mHandler = new Handler();
    }

    @Override
    public String getGameId() {
        return getResources().getString(R.string.city_quiz_game_id);
    }

    @Override
    public String getGameTitle() {
        return getResources().getString(R.string.cityquiz);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMapReady = true;
        mGoogleMap = googleMap;
        mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        mGoogleMap.setOnMarkerClickListener(this);
        loadInitialRound();
    }

    @Override
    public void onGlobalLayout() {
        mMapLaidOut = true;
        loadInitialRound();
    }

    /**
     * Load the initial round only if the map is ready and laid out and has not been already loaded.
     */
    private synchronized void loadInitialRound() {
        if (mMapLaidOut && mMapReady && !mInitialRoundLoaded) {
            loadRound();
            mInitialRoundLoaded = true;
        }
    }

    /**
     * Load the current round of the game for user interaction.
     */
    public void loadRound() {
        updateScore();

        // Get next city in game.
        final City city = mCityQuizGame.getCurrentRound().getCity();

        // Load city image
        loadCityImage(city.getImageName(), city.getImageAuthor());

        // Set up city markers
        mGoogleMap.clear();

        Log.d(TAG, "Moving to " + city.getName());
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(city.getBounds(),
                MapHelper.getMapPadding(mSupportMapFragment)));

        // Add markers and set appropriate tags.
        Marker locationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(city.getCorrectLocation())
                .icon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_blue)));
        locationMarker.setTag(CORRECT_MARKER);
        Marker firstFakeLocationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(city.getIncorrectLocation(City.FIRST_FAKE_INDEX))
                .icon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_blue)));
        firstFakeLocationMarker.setTag(FIRST_FAKE_MARKER);
        Marker secondFakeLocationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(city.getIncorrectLocation(City.SECOND_FAKE_INDEX))
                .icon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_blue)));
        secondFakeLocationMarker.setTag(SECOND_FAKE_MARKER);
    }

    /**
     * Load the image matching the given image name into the city ImageView.
     *
     * @param imageName Name used to retrieve the image from Firebase Storage.
     * @param imageAuthor Name used to give attribution for the image.
     */
    private void loadCityImage(final String imageName, final String imageAuthor) {
        // Clear current image
        Glide.with(CityQuizActivity.this)
                .load(R.color.cityQuizPrimaryGreenDark)
                .crossFade()
                .into(mCityImageView);

        mCityImageProgressBar.setVisibility(View.VISIBLE);
        showImageAuthor(false);
        showOnlineUI();

        // Load new image
        final Task<Uri> task = FirebaseStorage.getInstance().getReference().child(imageName).getDownloadUrl();
        task.addOnCompleteListener(this, new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        mCityImageProgressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            Uri uri = task.getResult();
                            Log.d(TAG, "Image uri: " + uri.toString());

                            final Context context = CityQuizActivity.this;
                            Glide.with(context)
                                    .load(uri)
                                    .crossFade()
                                    .listener(mCityImageGlideListener)
                                    .into(mCityImageView);

                            mCityImageAuthorTextView.setText(getResources().getString(R.string.photo_by, imageAuthor));
                            showImageAuthor(true);
                        } else {
                            Log.e(TAG, "Unable to get image URI from Firebase Storage. " + task.getException(),
                                    task.getException());
                            showOfflineUI();
                        }
                    }
                });

        // After timeout check if image URL has been retrieved, if not update UI.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!task.isComplete()) {
                    // Firebase was did not complete image URL retrieval.
                    mCityImageProgressBar.setVisibility(View.GONE);
                    showOfflineUI();
                }
            }
        }, IMAGE_LOAD_TIMEOUT_MILLIS);
    }

    private void showOfflineUI() {
        mCloudOffImageView.setVisibility(View.VISIBLE);
        mMapScrim.setVisibility(View.VISIBLE);
    }

    private void showOnlineUI() {
        mCloudOffImageView.setVisibility(View.GONE);
        mMapScrim.setVisibility(View.GONE);
    }

    private void updateScore() {
        // Update game information, round count and score.
        mRoundCountTextView.setText(getString(R.string.round_count_fmt, (mCityQuizGame.getCurrentRoundCount() + 1),
                mCityQuizGame.getTotalRoundCount()));
        mPointsTextView.setText(getString(R.string.game_score_fmt, mCityQuizGame.calculateScore()));
    }

    private void showImageAuthor(boolean visible) {
        if (visible) {
            mCityImageAuthorTextView.setVisibility(TextView.VISIBLE);
        } else {
            mCityImageAuthorTextView.setVisibility(TextView.INVISIBLE);
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        // Check if the round is already solved, if so ignore the marker click.
        if (mCityQuizGame.isFinished() || mCityQuizGame.getCurrentRound().isSolved()) {
            return true;
        }
        // Identify which marker was tapped and update the round status.
        int tag = (int)marker.getTag();
        mCityQuizGame.getCurrentRound().updateLocationStatus(tag, true);

        // Check if user tapped on the correct marker and if move to the next round.
        if (tag == 0) {
            marker.setIcon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_green));
            updateScore();

            // [ANALYTICS]
            int numIncorrectAttempts = 5 - mCityQuizGame.getCurrentRound().calculateRoundScore();
            MeasurementManager.recordCorrectCitySelected(mAnalytics,
                    mCityQuizGame.getCurrentRound().getCity().getImageName(),
                    numIncorrectAttempts);

            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(mCityQuizGame.getCurrentRound()
                    .getCity().getCorrectLocation()), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    showCityInfo(marker);
                }

                @Override
                public void onCancel() {
                    showCityInfo(marker);
                }
            });

            // Wait a while before moving to next round or end of game.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCityQuizGame.moveToNextRound();
                    // Check if the last round has completed.
                    if (!mCityQuizGame.isFinished()) {
                        loadRound();
                    } else {
                        // or exit. For now I will clear the markers.
                        goToGameSummary();
                        mPointsTextView.setText(getString(R.string.game_score_fmt, mCityQuizGame.calculateScore()));
                    }
                }
            }, NEXT_ROUND_DELAY);
        } else {
            // [ANALYTICS]
            MeasurementManager.recordIncorrectCitySelected(mAnalytics,
                    mCityQuizGame.getCurrentRound().getCity().getImageName());

            marker.setIcon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_red));
        }

        return true;
    }

    private void showCityInfo(Marker marker) {
        marker.setTitle(mCityQuizGame.getCurrentRound().getCity().getName());
        marker.showInfoWindow();
    }

    private void goToGameSummary() {
        // Show the end-game view
        EndOfGameView gameView = (EndOfGameView) findViewById(R.id.view_end_game);
        gameView.initialize(mCityQuizGame.calculateScore(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Restart this activity
                        recreate();
                    }

                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Back to the village
                        finish();
                    }
                });

        // Show end game view over everything
        gameView.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 21) {
            gameView.setZ(1000f);
        }
    }

    // Listener to handle Glide action completion.
    private static class CityImageGlideListener implements RequestListener {

        private WeakReference<CityQuizActivity> weakActivity;

        private CityImageGlideListener(CityQuizActivity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        @Override
        public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
            // Glide failed to load image.
            if (weakActivity.get() != null) {
                SantaLog.e(TAG, "Glide unable to load city image.");
                weakActivity.get().showOfflineUI();
            }
            return false;
        }

        @Override
        public boolean onResourceReady(Object resource, Object model, Target target,
                                       boolean isFromMemoryCache, boolean isFirstResource) {
            if (weakActivity.get() != null) {
                // Glide loaded image, hide "cloud off" ImageView and show author TextView.
                weakActivity.get().showOnlineUI();
                weakActivity.get().showImageAuthor(true);
            }
            return false;
        }
    }
}
