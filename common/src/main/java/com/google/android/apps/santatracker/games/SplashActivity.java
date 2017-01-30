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
package com.google.android.apps.santatracker.games;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.common.R;
import com.google.android.apps.santatracker.util.FontHelper;
import com.google.android.apps.santatracker.util.ImmersiveModeHelper;

/**
 * Splash screen for games. The splash screen rotates at runtime to match the orientation of the game
 * that will be launched.  This makes launching the splash screen jank-free from any orientation.
 */
public class SplashActivity extends AppCompatActivity {

    public static final int DELAY_MILLIS = 1000;

    private static final String TAG = "SplashActivity";
    private static final String EXTRA_SPLASH_IMAGE_ID = "extra_splash_image_id";
    private static final String EXTRA_SPLASH_TITLE_ID = "extra_splash_title_id";
    private static final String EXTRA_GAME_CLASS = "extra_game_class";
    private static final String EXTRA_LANDSCAPE = "extra_landscape";

    private Handler mHandler = new Handler();

    private Drawable mSplashImage;
    private String mSplashTitle;

    /**
     * Get an intent to launch a splash screen.
     * @param context launching context.
     * @param splashImageId resource ID for splash image.
     * @param splashTitleId resource ID for splash title.
     * @param isLandscape {@code true} if the game target is landscape only.
     * @param classToLaunch class of the game to launch.
     */
    public static Intent getIntent(Context context,
                                   @DrawableRes int splashImageId,
                                   @StringRes int splashTitleId,
                                   boolean isLandscape,
                                   Class classToLaunch) {

        Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra(EXTRA_SPLASH_IMAGE_ID, splashImageId);
        intent.putExtra(EXTRA_SPLASH_TITLE_ID, splashTitleId);
        intent.putExtra(EXTRA_LANDSCAPE, isLandscape);
        intent.putExtra(EXTRA_GAME_CLASS, classToLaunch);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Immersive mode (to hide nav).
        ImmersiveModeHelper.setImmersiveSticky(getWindow());

        // Get Views
        ImageView splashImageView = (ImageView) findViewById(R.id.splash_image);
        TextView splashTitleView = (TextView) findViewById(R.id.splash_title);

        // Set Image
        mSplashImage = ContextCompat.getDrawable(this, getIntent().getIntExtra(EXTRA_SPLASH_IMAGE_ID, -1));
        if (mSplashImage != null) {
            splashImageView.setImageDrawable(mSplashImage);
        }

        // Set Title
        mSplashTitle = getString(getIntent().getIntExtra(EXTRA_SPLASH_TITLE_ID, -1));
        splashTitleView.setText(mSplashTitle);

        // Make text "Lobster" font
        FontHelper.makeLobster(splashTitleView);
        
        // Start new activity in 1000ms
        final Class classToLaunch = (Class) getIntent().getSerializableExtra(EXTRA_GAME_CLASS);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, classToLaunch));
                finish();
            }
        }, DELAY_MILLIS);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Orientation
        boolean gameIsLandscape = getIntent().getBooleanExtra(EXTRA_LANDSCAPE, false);
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        // Figure out how many degrees to rotate
        // Landscape always wants to be at 90degrees, portrait always wants to be at 0degrees
        float degreesToRotate = 0f;
        if (rotation == Surface.ROTATION_0) {
            degreesToRotate = gameIsLandscape && !isLandscape? 90.0f : 0.0f;
        } else if (rotation == Surface.ROTATION_90) {
            degreesToRotate = gameIsLandscape && isLandscape? 0f : -90f;
        } else if (rotation == Surface.ROTATION_180) {
            degreesToRotate = gameIsLandscape && !isLandscape? -90f : -180f;
        } else if (rotation == Surface.ROTATION_270) {
            degreesToRotate = gameIsLandscape && isLandscape? -180f : -270f;
        }

        // On a TV, should always be 0
        if (isRunningOnTV()) {
            degreesToRotate = 0f;
        }

        // Rotate, if necessary
        if (degreesToRotate != 0) {
            Point size = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(size);
            } else {
                display.getSize(size);
            }
            int w = size.x;
            int h = size.y;

            View mainLayout = findViewById(R.id.splash_layout);
            mainLayout.setRotation(degreesToRotate);
            mainLayout.setTranslationX((w - h) / 2);
            mainLayout.setTranslationY((h - w) / 2);

            ViewGroup.LayoutParams lp = mainLayout.getLayoutParams();
            lp.height = w;
            lp.width = h;

            mainLayout.requestLayout();
        }
    }

    private boolean isRunningOnTV() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
    }
}
