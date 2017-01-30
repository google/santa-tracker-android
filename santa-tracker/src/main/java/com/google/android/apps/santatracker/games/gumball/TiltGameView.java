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

package com.google.android.apps.santatracker.games.gumball;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.apps.santatracker.R;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;

import java.util.Random;


/**
 * Custom view which contains the elements used in the physics word.
 * It handles the painting of all bitmaps for the game, including all levels (canes),
 * the pipe and gumballs.
 */
public class TiltGameView extends View {

    public static final float GUMBALL_DENSITY = 185.77f;
    public static final float GUMBALL_RADIUS = 0.258f;
    public static final float GUMBALL_BOUNCE = 0.2f;
    public static final float GUMBALL_FRICTION = 0.8f;
    /**
     * The physics world for the gumball game.
     */
    private PhysicsWorld mWorld;

    /**
     * Bitmaps for all elements on screen.
     */
    private Bitmap mGumballBlue;
    private Bitmap mGumballYellow;
    private Bitmap mGumballRed;
    private Bitmap mGumballGreen;
    private Bitmap mGumballOrange;
    private Bitmap mGumballPurple;
    private Bitmap mCaneMainLong;
    private Bitmap mCaneMainLongReverse;
    private Bitmap mCaneMainMed;
    private Bitmap mCaneMainMedReverse;
    private Bitmap mCaneMainSmall;
    private Bitmap mCaneMainSmallReverse;
    private Bitmap mCaneMainTiny;
    private Bitmap mCaneMainTinyReverse;
    private Bitmap mCaneHook;
    private Bitmap mCaneHookFlip;
    private Bitmap mCaneHookReverse;
    private Bitmap mCaneHookReverseFlip;
    private Bitmap mCaneEnd;
    private Bitmap mCaneEndFlip;
    private Bitmap mCaneEndReverse;
    private Bitmap mCaneEndReverseFlip;

    private Bitmap mCaneMainSmallAngleNine;
    private Bitmap mCaneMainSmallAngleSix;
    private Bitmap mCaneMainSmallAngleTwelve;
    private Bitmap mCaneMainReverseTinyAngleTwelve;
    private Bitmap mCaneMainLargeAngleSix;
    private Bitmap mCaneMainMedAngleSix;

    /**
     * Bitmap of a pipe where gumballs drop.
     */
    private Bitmap mPipeSides;

    /**
     * Default paint object that is used to draw all bitmaps to the screen.
     */
    private Paint mPaint = new Paint();

    /**
     * Identifiers for objects in the world.
     */
    public static final int GUMBALL_RED = 0;
    public static final int GUMBALL_BLUE = 1;
    public static final int GUMBALL_YELLOW = 2;
    public static final int GUMBALL_GREEN = 3;
    public static final int GUMBALL_ORANGE = 4;
    public static final int GUMBALL_PURPLE = 5;
    public static final int[] GUMBALLS = new int[]{GUMBALL_RED, GUMBALL_BLUE, GUMBALL_YELLOW,
            GUMBALL_GREEN, GUMBALL_ORANGE, GUMBALL_PURPLE};

    public static final int CANE_MAIN_LONG = 6;
    public static final int CANE_MAIN_LONG_REVERSE = 7;
    public static final int CANE_MAIN_MEDIUM = 8;
    public static final int CANE_MAIN_MEDIUM_REVERSE = 9;
    public static final int CANE_MAIN_SMALL = 10;
    public static final int CANE_MAIN_SMALL_REVERSE = 11;
    public static final int CANE_MAIN_TINY = 12;
    public static final int CANE_MAIN_TINY_REVERSE = 13;
    public static final int CANE_HOOK = 14;
    public static final int CANE_HOOK_FLIP = 15;
    public static final int CANE_HOOK_REVERSE = 16;
    public static final int CANE_HOOK_REVERSE_FLIP = 17;
    public static final int CANE_END = 18;
    public static final int CANE_END_FLIP = 19;
    public static final int CANE_END_REVERSE = 20;
    public static final int CANE_END_REVERSE_FLIP = 21;

    public static final int CANE_MAIN_SMALL_ANGLE_NINE = 22;
    public static final int CANE_MAIN_SMALL_ANGLE_SIX = 23;
    public static final int CANE_MAIN_SMALL_ANGLE_TWELVE = 24;
    public static final int CANE_MAIN_REVERSE_TINY_ANGLE_SIX = 25;
    public static final int CANE_MAIN_LARGE_ANGLE_SIX = 26;
    public static final int CANE_MAIN_MED_ANGLE_SIX = 27;

    public static final int PIPE_SIDES = -1;
    /**
     * Bottom of the pipe user data, this is separate from the side because the gumball is removed
     * from the scene on collision with it.
     */
    public static final int PIPE_BOTTOM = -2;

    public static final int GAME_FLOOR = -3;

    private static Random sRandomGenerator = new Random();

    private TiltGameFragment.GameCountdown mGameCountDown;

    public TiltGameView(Context context) {
        super(context);
        init();
    }

    public TiltGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Load the bitmaps as soon as we have the size of the view to scale them appropriately.
        if (mGumballBlue == null) {
            loadBitmaps();
        }
    }

    private void loadBitmaps() {
        // set the bitmaps
        Resources res = getResources();
        int[] gumballBlue = {R.drawable.gbg_gumball_blue_1920,
                R.drawable.gbg_gumball_blue_800, R.drawable.gbg_gumball_blue_480};
        int[] gumballRed = {R.drawable.gbg_gumball_red_1920,
                R.drawable.gbg_gumball_red_800, R.drawable.gbg_gumball_red_480};
        int[] gumballYellow = {R.drawable.gbg_gumball_yellow_1920,
                R.drawable.gbg_gumball_yellow_800,
                R.drawable.gbg_gumball_yellow_480};
        int[] gumballGreen = {R.drawable.gbg_gumball_green_1920,
                R.drawable.gbg_gumball_green_800,
                R.drawable.gbg_gumball_green_480};
        int[] gumballOrange = {R.drawable.gbg_gumball_orange_1920,
                R.drawable.gbg_gumball_orange_800,
                R.drawable.gbg_gumball_orange_480};
        int[] gumballPurple = {R.drawable.gbg_gumball_purple_1920,
                R.drawable.gbg_gumball_purple_800,
                R.drawable.gbg_gumball_purple_480};
        int[] caneMain = {R.drawable.gbg_candycane_main_1920,
                R.drawable.gbg_candycane_main_800,
                R.drawable.gbg_candycane_main_480};
        int[] caneMainReverse = {R.drawable.gbg_candycane_main_reverse_1920,

                R.drawable.gbg_candycane_main_reverse_800,
                R.drawable.gbg_candycane_main_reverse_480};
        int[] caneHook = {R.drawable.gbg_candycane_hook_1920,
                R.drawable.gbg_candycane_hook_800,
                R.drawable.gbg_candycane_hook_480};
        int[] caneHookReverse = {R.drawable.gbg_candycane_hook_reverse_1920,
                R.drawable.gbg_candycane_hook_reverse_800,
                R.drawable.gbg_candycane_hook_reverse_480};
        int[] caneEnd = {R.drawable.gbg_candycane_end_1920,
                R.drawable.gbg_candycane_end_800, R.drawable.gbg_candycane_end_480};
        int[] caneEndReverse = {R.drawable.gbg_candycane_end_reverse_1920,
                R.drawable.gbg_candycane_end_reverse_800,
                R.drawable.gbg_candycane_end_reverse_480};
        int[] pipes = {R.drawable.gbg_gumball_funnel_1920,
                R.drawable.gbg_gumball_funnel_800, R.drawable.gbg_gumball_funnel_480};
        int[] caneMainAngleNine = {R.drawable.gbg_candycane_main_angle_nine_1920,
                R.drawable.gbg_candycane_main_angle_nine_960,
                R.drawable.gbg_candycane_main_angle_nine_480};
        int[] caneMainAngleSix = {R.drawable.gbg_candycane_small_angle_six_1920,
                R.drawable.gbg_candycane_small_angle_six_960,
                R.drawable.gbg_candycane_small_angle_six_480};
        int[] caneMainAngleTwelve = {R.drawable.gbg_candycane_small_angle_twelve_1920,
                R.drawable.gbg_candycane_small_angle_twelve_960,
                R.drawable.gbg_candycane_small_angle_twelve_480};
        int[] caneMainTinyReverseAngleSix = {R.drawable.gbg_candycane_tiny_reverse_angle_six_1920,
                R.drawable.gbg_candycane_tiny_reverse_angle_six_960,
                R.drawable.gbg_candycane_tiny_reverse_angle_six_480};
        int[] caneMainLargeAngleSix = {R.drawable.gbg_candycane_large_angle_six_1920,
                R.drawable.gbg_candycane_large_angle_six_960,
                R.drawable.gbg_candycane_large_angle_six_480};
        int[] caneMainMedAngleSix = {R.drawable.gbg_candycane_med_angle_six_1920,
                R.drawable.gbg_candycane_med_angle_six_960,
                R.drawable.gbg_candycane_med_angle_six_480};
        int[] sizes = {1920, 960, 480};

        final int viewWidth = getWidth();
        int size = 0;
        for (int i = 1; i <= sizes.length; i++){
            if (viewWidth <= sizes[i-1]){
                size = i-1;
            } else {
                break;
            }
        }
        


        mGumballBlue = resizeImage(res, gumballBlue, sizes, size,  viewWidth, -360f, true, 1);
        mGumballRed = resizeImage(res, gumballRed, sizes, size,  viewWidth, -360f, true, 1);
        mGumballYellow = resizeImage(res, gumballYellow, sizes, size,  viewWidth, -360f, true, 1);
        mGumballGreen = resizeImage(res, gumballGreen, sizes, size,  viewWidth, -360f, true, 1);
        mGumballOrange = resizeImage(res, gumballOrange, sizes, size,  viewWidth, -360f, true, 1);
        mGumballPurple = resizeImage(res, gumballPurple, sizes, size,  viewWidth, -360f, true, 1);

        mCaneMainLong = resizeImage(res, caneMain, sizes, size,  viewWidth, 180f, false, 1);
        mCaneMainLongReverse = resizeImage(res, caneMainReverse, sizes, size,  viewWidth, 180f, false,
                1);
        mCaneMainMed = resizeImage(res, caneMain, sizes, size,  viewWidth, 180f, false, .75f);
        mCaneMainMedReverse = resizeImage(res, caneMainReverse, sizes, size,  viewWidth, 180f, false,
                .75f);

        mCaneMainSmall = resizeImage(res, caneMain, sizes, size,  viewWidth, 180f, false, .50f);
        mCaneMainSmallReverse = resizeImage(res, caneMainReverse, sizes, size,  viewWidth, 180f,
                false, .50f);
        mCaneMainTiny = resizeImage(res, caneMain, sizes, size,  viewWidth, 180f, false, .25f);
        mCaneMainTinyReverse = resizeImage(res, caneMainReverse, sizes, size,  viewWidth, 180f, false,
                .25f);

        mCaneMainSmallAngleNine = resizeImage(res, caneMainAngleNine, sizes, size,  viewWidth, 180f,
                true, 1f);
        mCaneMainSmallAngleSix = resizeImage(res, caneMainAngleSix, sizes, size,  viewWidth, 180f,
                true, 1f);
        mCaneMainSmallAngleTwelve = resizeImage(res, caneMainAngleTwelve, sizes, size,  viewWidth,
                180f, true, 1f);
        mCaneMainReverseTinyAngleTwelve = resizeImage(res, caneMainTinyReverseAngleSix,
                sizes, size,  viewWidth, 180f, true, 1f);
        mCaneMainLargeAngleSix = resizeImage(res, caneMainLargeAngleSix, sizes, size,  viewWidth,
                180f, true, 1f);
        mCaneMainMedAngleSix = resizeImage(res, caneMainMedAngleSix, sizes, size,  viewWidth, 180f,
                true, 1f);

        mCaneHook = resizeImage(res, caneHook, sizes, size,  viewWidth, 180f, false, 1);
        mCaneHookFlip = resizeImage(res, caneHook, sizes, size,  viewWidth, 180f, true, 1);
        mCaneHookReverse = resizeImage(res, caneHookReverse, sizes, size,  viewWidth, 180f, false, 1);
        mCaneHookReverseFlip = resizeImage(res, caneHookReverse, sizes, size,  viewWidth, 180f, true,
                1);
        mCaneEnd = resizeImage(res, caneEnd, sizes, size,  viewWidth, 180f, false, 1);
        mCaneEndFlip = resizeImage(res, caneEnd, sizes, size,  viewWidth, 180f, true, 1);
        mCaneEndReverse = resizeImage(res, caneEndReverse, sizes, size,  viewWidth, 180f, false, 1);
        mCaneEndReverseFlip = resizeImage(res, caneEndReverse, sizes, size,  viewWidth, 180f, true,
                1);
        mPipeSides = resizeImage(res, pipes, sizes, size,  viewWidth, 180f, true, 1);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Advance the countdown
        if (mGameCountDown != null) {
            mGameCountDown.tick();
        }

        // Load bitmaps if they haven't been initialised yet
        if (mGumballBlue == null) {
            loadBitmaps();
        }
        // reset and initialise the canvas for a fresh draw
        canvas.drawColor(Color.TRANSPARENT);
        canvas.translate(0, getHeight());
        canvas.scale(1.0f, -1.0f);
        float scale = getWidth() / 10.0f;
        mPaint.setAntiAlias(true);

        if (isInEditMode()) {
            return;
        }

        // Iterate through all of the bodies in the game world and draw the corresponding bitmaps
        Body body = mWorld.getWorld().getBodyList();
        while (body != null) {
            if (body.m_userData == null || body.m_userData.equals(PIPE_BOTTOM)) {
                body = body.getNext();
                continue;
            }
            // Skip bodies with empty fixtures or shapes
            Fixture fixture = body.getFixtureList();
            if (fixture == null) {
                body = body.getNext();
                continue;
            }
            Shape shape = fixture.getShape();
            if (shape == null) {
                body = body.getNext();
                continue;
            }

            // Get the position.
            Vec2 position = body.getPosition();

            // Get the bitmap of this body.
            Bitmap bitmap = null;
            if (body.getUserData() instanceof Gumball) {
                // For a gumball, load the correct color
                Gumball gumball = (Gumball) body.getUserData();
                if (gumball.mGumballColorId == GUMBALL_BLUE) {
                    bitmap = mGumballBlue;
                } else if (gumball.mGumballColorId == GUMBALL_YELLOW) {
                    bitmap = mGumballYellow;
                } else if (gumball.mGumballColorId == GUMBALL_RED) {
                    bitmap = mGumballRed;
                } else if (gumball.mGumballColorId == GUMBALL_GREEN) {
                    bitmap = mGumballGreen;
                } else if (gumball.mGumballColorId == GUMBALL_ORANGE) {
                    bitmap = mGumballOrange;
                } else if (gumball.mGumballColorId == GUMBALL_PURPLE) {
                    bitmap = mGumballPurple;
                }
            } else if (body.m_userData.equals(CANE_MAIN_LONG)) {
                bitmap = mCaneMainLong;
            } else if (body.m_userData.equals(CANE_MAIN_LONG_REVERSE)) {
                bitmap = mCaneMainLongReverse;
            } else if (body.m_userData.equals(CANE_MAIN_MEDIUM)) {
                bitmap = mCaneMainMed;
            } else if (body.m_userData.equals(CANE_MAIN_MEDIUM_REVERSE)) {
                bitmap = mCaneMainMedReverse;
            } else if (body.m_userData.equals(CANE_MAIN_SMALL)) {
                bitmap = mCaneMainSmall;
            } else if (body.m_userData.equals(CANE_MAIN_SMALL_REVERSE)) {
                bitmap = mCaneMainSmallReverse;
            } else if (body.m_userData.equals(CANE_MAIN_TINY)) {
                bitmap = mCaneMainTiny;
            } else if (body.m_userData.equals(CANE_MAIN_TINY_REVERSE)) {
                bitmap = mCaneMainTinyReverse;
            } else if (body.m_userData.equals(CANE_HOOK)) {
                bitmap = mCaneHook;
            } else if (body.m_userData.equals(CANE_HOOK_FLIP)) {
                bitmap = mCaneHookFlip;
            } else if (body.m_userData.equals(CANE_HOOK_REVERSE)) {
                bitmap = mCaneHookReverse;
            } else if (body.m_userData.equals(CANE_HOOK_REVERSE_FLIP)) {
                bitmap = mCaneHookReverseFlip;
            } else if (body.m_userData.equals(CANE_END)) {
                bitmap = mCaneEnd;
            } else if (body.m_userData.equals(CANE_END_FLIP)) {
                bitmap = mCaneEndFlip;
            } else if (body.m_userData.equals(CANE_END_REVERSE)) {
                bitmap = mCaneEndReverse;
            } else if (body.m_userData.equals(CANE_END_REVERSE_FLIP)) {
                bitmap = mCaneEndReverseFlip;
            } else if (body.m_userData.equals(PIPE_SIDES)) {
                bitmap = mPipeSides;
            } else if (body.m_userData.equals(CANE_MAIN_SMALL_ANGLE_NINE)) {
                bitmap = mCaneMainSmallAngleNine;
            } else if (body.m_userData.equals(CANE_MAIN_SMALL_ANGLE_SIX)) {
                bitmap = mCaneMainSmallAngleSix;
            } else if (body.m_userData.equals(CANE_MAIN_SMALL_ANGLE_TWELVE)) {
                bitmap = mCaneMainSmallAngleTwelve;
            } else if (body.m_userData.equals(CANE_MAIN_REVERSE_TINY_ANGLE_SIX)) {
                bitmap = mCaneMainReverseTinyAngleTwelve;
            } else if (body.m_userData.equals(CANE_MAIN_LARGE_ANGLE_SIX)) {
                bitmap = mCaneMainLargeAngleSix;
            } else if (body.m_userData.equals(CANE_MAIN_MED_ANGLE_SIX)) {
                bitmap = mCaneMainMedAngleSix;
            }

            if (shape instanceof CircleShape && bitmap != null) {
                // Draw a gumball
                CircleShape circleShape = (CircleShape) shape;
                canvas.save();
                canvas.rotate((float) (180 * body.getAngle() / Math.PI), scale * position.x,
                        scale * position.y);
                canvas.drawBitmap(bitmap, scale * (position.x - circleShape.m_radius),
                        scale * (position.y - circleShape.m_radius), mPaint);
                canvas.restore();

            } else if (shape instanceof EdgeShape && bitmap != null) {
                // Draw all other objects
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.rotate((float) (180 * body.getAngle() / Math.PI), scale * position.x,
                        scale * position.y);
                canvas.drawBitmap(bitmap, scale * (position.x), scale * (position.y), mPaint);
                canvas.restore();

            }

            // Continue drawing with the next body in the world
            body = body.getNext();
        }
    }

    public void setGameCountDown(TiltGameFragment.GameCountdown gameCountDown) {
        mGameCountDown = gameCountDown;
    }

    /**
     * Returns the index of a randomly colored gumball.
     */
    public static int getRandomGumballId() {
        int index = sRandomGenerator.nextInt(GUMBALLS.length);
        int id = GUMBALLS[index];
        return id;
    }

    /**
     * Gets the correct bitmap based on screen size and rotates and flips the image.
     */
    private static Bitmap resizeImage(Resources res, int[] resourceId, int[] sizes, int size, int viewWidth,
            float rotationDegrees, boolean isFlipped, float caneScale) {

        Matrix matrix = new Matrix();
        Bitmap bmp = BitmapFactory.decodeResource(res, resourceId[size]);

        if (rotationDegrees != 361f) {
            matrix.setRotate(rotationDegrees, bmp.getWidth() / 2, bmp.getHeight() / 2);
        }
        if (isFlipped) {
            matrix.preScale(-1, 1);
        }
        float scale = ((float) viewWidth) / sizes[size];
        matrix.postScale(scale, scale);

        bmp = Bitmap
                .createBitmap(bmp, 0, 0, (int) (bmp.getWidth() * caneScale), bmp.getHeight(),
                        matrix, true);

        return bmp;
    }

    /**
     * Sets the Box 2D physics world to draw.
     */
    public void setModel(PhysicsWorld world) {
        mWorld = world;
    }

    /**
     * Adds a gumball for drawing.
     */
    public void addGumball(Gumball gumball) {
        gumball.mGumballColorId = getRandomGumballId();
        mWorld.addGumball(gumball.mXInitPos, gumball.mYInitPos, gumball, GUMBALL_DENSITY,
                GUMBALL_RADIUS, GUMBALL_BOUNCE, GUMBALL_FRICTION, BodyType.DYNAMIC);
    }

}
