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

package com.google.android.apps.santatracker.games.jetpack;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.gamebase.GameConfig;
import com.google.android.apps.santatracker.games.simpleengine.Renderer;
import com.google.android.apps.santatracker.games.simpleengine.game.GameObject;
import com.google.android.apps.santatracker.games.simpleengine.game.World;

import java.util.GregorianCalendar;
import java.util.Random;

public class JetpackObjectFactory {

    Renderer mRenderer;
    World mWorld;
    Random mRandom = new Random();

    // item subtypes
    public static final int ITEM_PRESENT = 0;
    public static final int ITEM_CANDY = 1;
    public static final int ITEM_COAL = 2;

    // Textures
    int mTexPlayer;
    int[] mTexItemCandy;
    int[] mTexItemCoal;
    int[] mTexItemPresent;
    int mTexCloud;
    int[] mComboTex;
    int mTexFire;
    int mTexPlayerHitOverlay;
    int mTexPlayerHit;
    int mTexBackground;

    JetpackObjectFactory(Renderer r, World w) {
        mRenderer = r;
        mWorld = w;
    }

    GameObject makePlayer() {
        GameObject p = mWorld
                .newGameObjectWithImage(JetpackScene.TYPE_PLAYER, 0.0f, mRenderer.getBottom() +
                        JetpackConfig.Player.VERT_MOVEMENT_MARGIN, mTexPlayer,
                        JetpackConfig.Player.WIDTH, Float.NaN);
        p.setBoxCollider(JetpackConfig.Player.COLLIDER_WIDTH,
                JetpackConfig.Player.COLLIDER_HEIGHT);

        Renderer.Sprite fireSprite = p.getSprite(p.addSprite());
        fireSprite.texIndex = mTexFire;
        fireSprite.width = JetpackConfig.Player.Fire.WIDTH;
        fireSprite.height = Float.NaN;
        fireSprite.tintFactor = 0.0f;
        return p;
    }

    void makePlayerHit(GameObject p) {
        p.deleteSprites();

        Renderer.Sprite playerHitSprite = p.getSprite(p.addSprite());
        playerHitSprite.texIndex = mTexPlayerHit;
        playerHitSprite.width = JetpackConfig.Player.INJURED_WIDTH;
        playerHitSprite.height = Float.NaN;
        playerHitSprite.tintFactor = 0.0f;

        Renderer.Sprite fireSprite = p.getSprite(p.addSprite());
        fireSprite.texIndex = mTexFire;
        fireSprite.width = JetpackConfig.Player.Fire.WIDTH;
        fireSprite.height = Float.NaN;
        fireSprite.tintFactor = 0.0f;
    }

    void recoverPlayerHit(GameObject p) {
        p.deleteSprites();

        Renderer.Sprite playerSprite = p.getSprite(p.addSprite());
        playerSprite.texIndex = mTexPlayer;
        playerSprite.width = JetpackConfig.Player.WIDTH;
        playerSprite.height = Float.NaN;
        playerSprite.tintFactor = 0.0f;

        Renderer.Sprite fireSprite = p.getSprite(p.addSprite());
        fireSprite.texIndex = mTexFire;
        fireSprite.width = JetpackConfig.Player.Fire.WIDTH;
        fireSprite.height = Float.NaN;
        fireSprite.tintFactor = 0.0f;
    }

    public int getItemTypeGivenProbs(float coal, float candy, float presents) {
        float randFloat = mRandom.nextFloat();
        if(randFloat < coal) {
            return ITEM_COAL;
        } else if(randFloat < coal + candy) {
            return ITEM_CANDY;
        } else {
            return ITEM_PRESENT;
        }
    }

    public int getItemType(boolean bigPresentMode, float currentScore) {
        if (bigPresentMode) {
            if (currentScore < 10) {
                return getItemTypeGivenProbs(0, 0, 1.00f);
            } else if (currentScore < 20) {
                return getItemTypeGivenProbs(.1f, .4f, .5f);
            } else if (currentScore < 30) {
                return getItemTypeGivenProbs(.15f, .45f, .40f);
            } else if (currentScore < 40) {
                return getItemTypeGivenProbs(.2f, .45f, .35f);
            } else if (currentScore < 50) {
                return getItemTypeGivenProbs(.25f, .45f, .3f);
            } else {
                return getItemTypeGivenProbs(.3f, .4f, .30f);
            }
        } else {
            if (currentScore < 10) {
                return getItemTypeGivenProbs(0, 0.25f, 0.75f);
            } else if (currentScore < 20) {
                return getItemTypeGivenProbs(.1f, .55f, .35f);
            } else if (currentScore < 30) {
                return getItemTypeGivenProbs(.15f, .60f, .25f);
            } else if (currentScore < 40) {
                return getItemTypeGivenProbs(.2f, .60f, .20f);
            } else if (currentScore < 50) {
                return getItemTypeGivenProbs(.25f, .55f, .2f);
            } else {
                return getItemTypeGivenProbs(.3f, .50f, .2f);
            }
        }
    }

    public GameObject makeBackground() {
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, 0.0f, 0.0f, mTexBackground,
                mRenderer.getWidth() + 0.02f, mRenderer.getHeight() + 0.02f);
    }

    GameObject makeRandomItem(float fallSpeedMultiplier, boolean bigPresentMode, float currentScore) {
        float minX = mRenderer.getLeft() + 2 * JetpackConfig.Items.PRESENT_WIDTH;
        float maxX = mRenderer.getRight() - 2 * JetpackConfig.Items.PRESENT_WIDTH;
        float x = minX + mRandom.nextFloat() * (maxX - minX);
        // 0 is candy, 1 is coal, 2 is present
        int itemType = getItemType(bigPresentMode, currentScore);
        int itemSubtype = mRandom.nextInt(4); // one of the 4 subtypes

        int tex;
        float width;
        float colliderWidth, colliderHeight;
        GameObject p = null;
        switch (itemType) {
            case ITEM_CANDY:
                tex = mTexItemCandy[itemSubtype];
                width = JetpackConfig.Items.CANDY_WIDTH;
                colliderWidth = JetpackConfig.Items.CANDY_COLLIDER_WIDTH;
                colliderHeight = JetpackConfig.Items.CANDY_COLLIDER_HEIGHT;
                p =  mWorld.
                        newGameObjectWithImage(JetpackScene.TYPE_GOOD_ITEM, x,
                                JetpackConfig.Items.SPAWN_Y, tex, width, Float.NaN);
                p.ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = JetpackConfig.Items.BASE_VALUE;
                break;
            case ITEM_COAL:
                tex = mTexItemCoal[0];
                width = JetpackConfig.Items.SMALL_WIDTH;
                colliderWidth = JetpackConfig.Items.SMALL_COLLIDER_WIDTH;
                colliderHeight = JetpackConfig.Items.SMALL_COLLIDER_HEIGHT;
                p =  mWorld.
                        newGameObjectWithImage(JetpackScene.TYPE_BAD_ITEM, x,
                                JetpackConfig.Items.SPAWN_Y, tex, width, Float.NaN);
                p.ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = -JetpackConfig.Items.BASE_VALUE;
                break;
            case ITEM_PRESENT:
            default:
                tex = mTexItemPresent[itemSubtype];
                width = JetpackConfig.Items.PRESENT_WIDTH;
                colliderWidth = JetpackConfig.Items.PRESENT_COLLIDER_WIDTH;
                colliderHeight = JetpackConfig.Items.PRESENT_COLLIDER_HEIGHT;
                p = mWorld.
                        newGameObjectWithImage(JetpackScene.TYPE_GOOD_ITEM, x,
                            JetpackConfig.Items.SPAWN_Y, tex, width, Float.NaN);
                p.ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = JetpackConfig.Items.BASE_VALUE * 2;
                break;
        }

        p.velY = -(JetpackConfig.Items.FALL_SPEED_MIN + mRandom.nextFloat() *
                (JetpackConfig.Items.FALL_SPEED_MAX - JetpackConfig.Items.FALL_SPEED_MIN));
        p.velY *= fallSpeedMultiplier;
        p.setBoxCollider(colliderWidth, colliderHeight);
        p.ivar[JetpackConfig.Items.IVAR_TYPE] = itemType;
        p.bringToFront();
        return p;
    }

    GameObject makeCloud() {
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, 0.0f, 0.0f, mTexCloud,
                JetpackConfig.Clouds.WIDTH, Float.NaN);
    }

    GameObject makeComboPopup(int comboItems, float x, float y) {
        int i = comboItems - 2;
        i = i < 0 ? 0 : i >= mComboTex.length ? mComboTex.length - 1 : i;
        GameObject o = mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y,
                mComboTex[i], JetpackConfig.ComboPopup.SIZE, Float.NaN);
        o.velY = JetpackConfig.ComboPopup.VEL_Y;
        o.timeToLive = GameConfig.ScorePopup.POPUP_EXPIRE;
        return o;
    }

    protected void requestTextures() {
        // request player texture
        mTexPlayer = mRenderer.requestImageTex(R.drawable.jetpack_player, "jetpack_player",
                Renderer.DIM_WIDTH, JetpackConfig.Player.WIDTH);

        // request item textures
        mTexItemCandy = new int[4];
        int i = 0;
        for (int resId : new int[]{R.drawable.jetpack_candy1, R.drawable.jetpack_candy2,
                R.drawable.jetpack_candy3, R.drawable.jetpack_candy4}) {
            mTexItemCandy[i++] = mRenderer.requestImageTex(resId, "candy", Renderer.DIM_WIDTH,
                    JetpackConfig.Items.CANDY_WIDTH);
        }
        mTexItemPresent = new int[4];
        i = 0;
        for (int resId : new int[]{R.drawable.jetpack_present1, R.drawable.jetpack_present2,
                R.drawable.jetpack_present3, R.drawable.jetpack_present4}) {
            mTexItemPresent[i++] = mRenderer.requestImageTex(resId, "present", Renderer.DIM_WIDTH,
                    JetpackConfig.Items.PRESENT_WIDTH);
        }

        i = 0;
        int[] coalDrawables = new int[]{ R.drawable.jetpack_coal };
        mTexItemCoal = new int[coalDrawables.length];

        for (int resId : coalDrawables) {
            mTexItemCoal[i++] = mRenderer.requestImageTex(resId, "small", Renderer.DIM_WIDTH,
                    JetpackConfig.Items.SMALL_WIDTH);
        }

        mTexCloud = mRenderer.requestImageTex(R.drawable.jetpack_cloud, "jetpack_cloud",
                Renderer.DIM_WIDTH, JetpackConfig.Clouds.WIDTH);

        mTexBackground = mRenderer.requestImageTex(getBackgroundFromCurrentTime(),
                "jetpack_background", Renderer.DIM_WIDTH, mRenderer.getWidth());

        mComboTex = new int[3];
        mComboTex[0] = mRenderer.requestImageTex(R.drawable.jetpack_combo_2x, "jetpack_combo_2x",
                Renderer.DIM_WIDTH, JetpackConfig.ComboPopup.SIZE);
        mComboTex[1] = mRenderer.requestImageTex(R.drawable.jetpack_combo_3x, "jetpack_combo_3x",
                Renderer.DIM_WIDTH, JetpackConfig.ComboPopup.SIZE);
        mComboTex[2] = mRenderer.requestImageTex(R.drawable.jetpack_combo_4x, "jetpack_combo_4x",
                Renderer.DIM_WIDTH, JetpackConfig.ComboPopup.SIZE);
        mTexFire = mRenderer.requestImageTex(R.drawable.jetpack_fire, "jetpack_fire",
                Renderer.DIM_WIDTH, JetpackConfig.Player.Fire.WIDTH);
        mTexPlayerHit = mRenderer.requestImageTex(R.drawable.jetpack_player_hit,
                "jetpack_player_hit", Renderer.DIM_WIDTH, JetpackConfig.Player.WIDTH);
        mTexPlayerHitOverlay = mRenderer.requestImageTex(R.drawable.jetpack_player_hit_overlay,
                "jetpack_player_hit_overlay", Renderer.DIM_WIDTH, JetpackConfig.Player.WIDTH);
    }

    public int getBackgroundFromCurrentTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        int hour = calendar.get(GregorianCalendar.HOUR_OF_DAY);
        if(hour < 21 && hour > 5) {
            return R.drawable.jetpack_background_day;
        } else {
            return R.drawable.jetpack_background_evening;
        }
    }
}
