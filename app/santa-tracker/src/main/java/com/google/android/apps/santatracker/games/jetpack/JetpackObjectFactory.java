/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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
import com.google.android.apps.santatracker.games.jetpack.gamebase.GameConfig;
import com.google.android.apps.santatracker.games.simpleengine.Renderer;
import com.google.android.apps.santatracker.games.simpleengine.game.GameObject;
import com.google.android.apps.santatracker.games.simpleengine.game.World;

import java.util.Random;

public class JetpackObjectFactory {

    Renderer mRenderer;
    World mWorld;
    Random mRandom = new Random();

    // item subtypes
    public static final int ITEM_PRESENT = 0;
    public static final int ITEM_CANDY = 1;
    public static final int ITEM_SMALL = 2;

    // Textures
    int mTexPlayer;
    int[] mTexItemCandy;
    int[] mTexItemSmall;
    int[] mTexItemPresent;
    int mTexCloud;
    int[] mComboTex;
    int mTexFire;

    JetpackObjectFactory(Renderer r, World w) {
        mRenderer = r;
        mWorld = w;
    }

    GameObject makePlayer() {
        GameObject p = mWorld
                .newGameObjectWithImage(JetpackScene.TYPE_PLAYER, 0.0f, 0.0f, mTexPlayer,
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

    GameObject makeRandomItem(float fallSpeedMultiplier) {
        float minX = mRenderer.getLeft() + 2 * JetpackConfig.Items.PRESENT_WIDTH;
        float maxX = mRenderer.getRight() - 2 * JetpackConfig.Items.PRESENT_WIDTH;
        float x = minX + mRandom.nextFloat() * (maxX - minX);

        // 0 is candy, 1 is small item, 2 is present
        int itemType = mRandom.nextInt(3);
        int itemSubtype = mRandom.nextInt(4); // one of the 4 subtypes

        int tex;
        float width;
        float colliderWidth, colliderHeight;
        boolean isLarge = false;

        switch (itemType) {
            case ITEM_CANDY:
                tex = mTexItemCandy[itemSubtype];
                width = JetpackConfig.Items.CANDY_WIDTH;
                colliderWidth = JetpackConfig.Items.CANDY_COLLIDER_WIDTH;
                colliderHeight = JetpackConfig.Items.CANDY_COLLIDER_HEIGHT;
                break;
            case ITEM_SMALL:
                tex = mTexItemSmall[itemSubtype];
                width = JetpackConfig.Items.SMALL_WIDTH;
                colliderWidth = JetpackConfig.Items.SMALL_COLLIDER_WIDTH;
                colliderHeight = JetpackConfig.Items.SMALL_COLLIDER_HEIGHT;
                break;
            default:
                tex = mTexItemPresent[itemSubtype];
                width = JetpackConfig.Items.PRESENT_WIDTH;
                colliderWidth = JetpackConfig.Items.PRESENT_COLLIDER_WIDTH;
                colliderHeight = JetpackConfig.Items.PRESENT_COLLIDER_HEIGHT;
                isLarge = true;
                break;
        }

        GameObject p = mWorld
                .newGameObjectWithImage(JetpackScene.TYPE_ITEM, x, JetpackConfig.Items.SPAWN_Y,
                        tex, width, Float.NaN);

        p.velY = -(JetpackConfig.Items.FALL_SPEED_MIN + mRandom.nextFloat() *
                (JetpackConfig.Items.FALL_SPEED_MAX - JetpackConfig.Items.FALL_SPEED_MIN));
        p.velY *= fallSpeedMultiplier;
        p.setBoxCollider(colliderWidth, colliderHeight);
        p.ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = isLarge ? JetpackConfig.Items.BASE_VALUE * 2 :
                JetpackConfig.Items.BASE_VALUE;
        p.ivar[JetpackConfig.Items.IVAR_TYPE] = itemType;
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
        mTexItemSmall = new int[4];
        i = 0;
        for (int resId : new int[]{R.drawable.jetpack_small1, R.drawable.jetpack_small2,
                R.drawable.jetpack_small3, R.drawable.jetpack_small4}) {
            mTexItemSmall[i++] = mRenderer.requestImageTex(resId, "small", Renderer.DIM_WIDTH,
                    JetpackConfig.Items.SMALL_WIDTH);
        }

        mTexCloud = mRenderer.requestImageTex(R.drawable.jetpack_cloud, "jetpack_cloud",
                Renderer.DIM_WIDTH, JetpackConfig.Clouds.WIDTH);

        mComboTex = new int[3];
        mComboTex[0] = mRenderer.requestImageTex(R.drawable.jetpack_combo_2x, "jetpack_combo_2x",
                Renderer.DIM_WIDTH, JetpackConfig.ComboPopup.SIZE);
        mComboTex[1] = mRenderer.requestImageTex(R.drawable.jetpack_combo_3x, "jetpack_combo_3x",
                Renderer.DIM_WIDTH, JetpackConfig.ComboPopup.SIZE);
        mComboTex[2] = mRenderer.requestImageTex(R.drawable.jetpack_combo_4x, "jetpack_combo_4x",
                Renderer.DIM_WIDTH, JetpackConfig.ComboPopup.SIZE);
        mTexFire = mRenderer.requestImageTex(R.drawable.jetpack_fire, "jetpack_fire",
                Renderer.DIM_WIDTH, JetpackConfig.Player.Fire.WIDTH);
    }
}
