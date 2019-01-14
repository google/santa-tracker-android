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

package com.google.android.apps.jetpack

import com.google.android.apps.playgames.simpleengine.Renderer
import com.google.android.apps.playgames.simpleengine.game.GameObject
import com.google.android.apps.playgames.simpleengine.game.World
import java.util.GregorianCalendar
import java.util.Random

class JetpackObjectFactory internal constructor(
    private var renderer: Renderer,
    private var world: World
) {
    private val random = Random()

    // Textures
    private var texPlayer: Int = 0
    private var texItemCandy: IntArray? = null
    private var texItemCoal: IntArray? = null
    private var texItemPresent: IntArray? = null
    private var texCloud: Int = 0
    private var comboTex: IntArray? = null
    private var texFire: Int = 0
    private var texPlayerHitOverlay: Int = 0
    private var texPlayerHit: Int = 0
    private var texBackground: Int = 0

    private val backgroundFromCurrentTime: Int
        get() {
            val calendar = GregorianCalendar()
            val hour = calendar.get(GregorianCalendar.HOUR_OF_DAY)
            return if (hour in 6..20) {
                com.google.android.apps.santatracker.common.R.drawable.jetpack_background_day
            } else {
                com.google
                        .android
                        .apps
                        .santatracker
                        .common
                        .R
                        .drawable
                        .jetpack_background_evening
            }
        }

    internal fun makePlayer(): GameObject {
        val p = world.newGameObjectWithImage(
                JetpackScene.TYPE_PLAYER,
                0.0f,
                renderer.bottom + JetpackConfig.Player.VERT_MOVEMENT_MARGIN,
                texPlayer,
                JetpackConfig.Player.WIDTH,
                java.lang.Float.NaN)
        p.setBoxCollider(JetpackConfig.Player.COLLIDER_WIDTH, JetpackConfig.Player.COLLIDER_HEIGHT)

        p.getSprite(p.addSprite()).apply {
            texIndex = texFire
            width = JetpackConfig.Player.Fire.WIDTH
            height = java.lang.Float.NaN
            tintFactor = 0.0f
        }
        return p
    }

    internal fun makePlayerHit(p: GameObject) {
        p.deleteSprites()

        p.getSprite(p.addSprite()).apply {
            texIndex = texPlayerHit
            width = JetpackConfig.Player.INJURED_WIDTH
            height = java.lang.Float.NaN
            tintFactor = 0.0f
        }

        p.getSprite(p.addSprite()).apply {
            texIndex = texFire
            width = JetpackConfig.Player.Fire.WIDTH
            height = java.lang.Float.NaN
            tintFactor = 0.0f
        }
    }

    internal fun recoverPlayerHit(p: GameObject) {
        p.deleteSprites()

        p.getSprite(p.addSprite()).apply {
            texIndex = texPlayer
            width = JetpackConfig.Player.WIDTH
            height = java.lang.Float.NaN
            tintFactor = 0.0f
        }

        p.getSprite(p.addSprite()).apply {
            texIndex = texFire
            width = JetpackConfig.Player.Fire.WIDTH
            height = java.lang.Float.NaN
            tintFactor = 0.0f
        }
    }

    private fun getItemTypeGivenProbs(coal: Float, candy: Float, presents: Float): Int {
        val randFloat = random.nextFloat()
        return when {
            randFloat < coal -> ITEM_COAL
            randFloat < coal + candy -> ITEM_CANDY
            else -> ITEM_PRESENT
        }
    }

    private fun getItemType(bigPresentMode: Boolean, currentScore: Float): Int {
        return if (bigPresentMode) {
            when {
                currentScore < 10 -> getItemTypeGivenProbs(0f, 0f, 1.00f)
                currentScore < 20 -> getItemTypeGivenProbs(.1f, .4f, .5f)
                currentScore < 30 -> getItemTypeGivenProbs(.15f, .45f, .40f)
                currentScore < 40 -> getItemTypeGivenProbs(.2f, .45f, .35f)
                currentScore < 50 -> getItemTypeGivenProbs(.25f, .45f, .3f)
                else -> getItemTypeGivenProbs(.3f, .4f, .30f)
            }
        } else {
            when {
                currentScore < 10 -> getItemTypeGivenProbs(0f, 0.25f, 0.75f)
                currentScore < 20 -> getItemTypeGivenProbs(.1f, .55f, .35f)
                currentScore < 30 -> getItemTypeGivenProbs(.15f, .60f, .25f)
                currentScore < 40 -> getItemTypeGivenProbs(.2f, .60f, .20f)
                currentScore < 50 -> getItemTypeGivenProbs(.25f, .55f, .2f)
                else -> getItemTypeGivenProbs(.3f, .50f, .2f)
            }
        }
    }

    fun makeBackground(): GameObject {
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR,
                0.0f,
                0.0f,
                texBackground,
                renderer.width + 0.02f,
                renderer.height + 0.02f)
    }

    internal fun makeRandomItem(
        fallSpeedMultiplier: Float,
        bigPresentMode: Boolean,
        currentScore: Float
    ): GameObject {
        val minX = renderer.left + 2 * JetpackConfig.Items.PRESENT_WIDTH
        val maxX = renderer.right - 2 * JetpackConfig.Items.PRESENT_WIDTH
        val x = minX + random.nextFloat() * (maxX - minX)
        // 0 is candy, 1 is coal, 2 is present
        val itemType = getItemType(bigPresentMode, currentScore)
        val itemSubtype = random.nextInt(4) // one of the 4 subtypes

        val tex: Int
        val width: Float
        val colliderWidth: Float
        val colliderHeight: Float
        var p: GameObject?
        when (itemType) {
            ITEM_CANDY -> {
                val texItemCandySnapshot = texItemCandy ?: throw IllegalStateException()
                tex = texItemCandySnapshot[itemSubtype]
                width = JetpackConfig.Items.CANDY_WIDTH
                colliderWidth = JetpackConfig.Items.CANDY_COLLIDER_WIDTH
                colliderHeight = JetpackConfig.Items.CANDY_COLLIDER_HEIGHT
                p = world.newGameObjectWithImage(
                        JetpackScene.TYPE_GOOD_ITEM,
                        x,
                        JetpackConfig.Items.SPAWN_Y,
                        tex,
                        width,
                        java.lang.Float.NaN).apply {
                    ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = JetpackConfig.Items.BASE_VALUE
                }
            }
            ITEM_COAL -> {
                val texItemCoalSnapshot = texItemCoal ?: throw IllegalStateException()
                tex = texItemCoalSnapshot[0]
                width = JetpackConfig.Items.SMALL_WIDTH
                colliderWidth = JetpackConfig.Items.SMALL_COLLIDER_WIDTH
                colliderHeight = JetpackConfig.Items.SMALL_COLLIDER_HEIGHT
                p = world.newGameObjectWithImage(
                        JetpackScene.TYPE_BAD_ITEM,
                        x,
                        JetpackConfig.Items.SPAWN_Y,
                        tex,
                        width,
                        java.lang.Float.NaN).apply {
                    ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = -JetpackConfig.Items.BASE_VALUE
                }
            }
            ITEM_PRESENT -> {
                val texItemPresentSnapshot = texItemPresent ?: throw IllegalStateException()
                tex = texItemPresentSnapshot[itemSubtype]
                width = JetpackConfig.Items.PRESENT_WIDTH
                colliderWidth = JetpackConfig.Items.PRESENT_COLLIDER_WIDTH
                colliderHeight = JetpackConfig.Items.PRESENT_COLLIDER_HEIGHT
                p = world.newGameObjectWithImage(
                        JetpackScene.TYPE_GOOD_ITEM,
                        x,
                        JetpackConfig.Items.SPAWN_Y,
                        tex,
                        width,
                        java.lang.Float.NaN).apply {
                    ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = JetpackConfig.Items.BASE_VALUE * 2
                }
            }
            else -> {
                val texItemPresentSnapshot = texItemPresent ?: throw IllegalStateException()
                tex = texItemPresentSnapshot[itemSubtype]
                width = JetpackConfig.Items.PRESENT_WIDTH
                colliderWidth = JetpackConfig.Items.PRESENT_COLLIDER_WIDTH
                colliderHeight = JetpackConfig.Items.PRESENT_COLLIDER_HEIGHT
                p = world.newGameObjectWithImage(JetpackScene.TYPE_GOOD_ITEM, x,
                        JetpackConfig.Items.SPAWN_Y, tex, width, java.lang.Float.NaN).apply {
                    ivar[JetpackConfig.Items.IVAR_BASE_VALUE] = JetpackConfig.Items.BASE_VALUE * 2
                }
            }
        }

        p.velY =
                -(JetpackConfig.Items.FALL_SPEED_MIN + random.nextFloat() * (JetpackConfig.Items.FALL_SPEED_MAX - JetpackConfig.Items.FALL_SPEED_MIN))
        p.velY *= fallSpeedMultiplier
        p.setBoxCollider(colliderWidth, colliderHeight)
        p.ivar[JetpackConfig.Items.IVAR_TYPE] = itemType
        p.bringToFront()
        return p
    }

    internal fun makeCloud(): GameObject {
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR,
                0.0f,
                0.0f,
                texCloud,
                JetpackConfig.Clouds.WIDTH,
                java.lang.Float.NaN)
    }

    internal fun makeComboPopup(comboItems: Int, x: Float, y: Float): GameObject {
        val comboTex = comboTex ?: throw IllegalStateException()
        var i = comboItems - 2
        i = if (i < 0) 0 else if (i >= comboTex.size) comboTex.size - 1 else i
        val o = world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR,
                x,
                y,
                comboTex[i],
                JetpackConfig.ComboPopup.SIZE,
                java.lang.Float.NaN)
        o.velY = JetpackConfig.ComboPopup.VEL_Y
        o.timeToLive = GameConfig.ScorePopup.POPUP_EXPIRE
        return o
    }

    fun requestTextures() {
        // request player texture
        texPlayer = renderer.requestImageTex(
                R.drawable.jetpack_player,
                "jetpack_player",
                Renderer.DIM_WIDTH,
                JetpackConfig.Player.WIDTH)

        // request item textures
        texItemCandy = IntArray(4)
        val texItemCandySnapshot = texItemCandy ?: throw IllegalStateException()
        var i = 0
        for (resId in intArrayOf(R.drawable.jetpack_candy1, R.drawable.jetpack_candy2,
                R.drawable.jetpack_candy3, R.drawable.jetpack_candy4)) {
            texItemCandySnapshot[i++] = renderer.requestImageTex(
                    resId, "candy", Renderer.DIM_WIDTH, JetpackConfig.Items.CANDY_WIDTH)
        }
        texItemPresent = IntArray(4)
        val texItemPresentSnapshot = texItemPresent ?: throw IllegalStateException()
        i = 0
        for (resId in intArrayOf(R.drawable.jetpack_present1, R.drawable.jetpack_present2,
                R.drawable.jetpack_present3, R.drawable.jetpack_present4)) {
            texItemPresentSnapshot[i++] = renderer.requestImageTex(
                    resId,
                    "present",
                    Renderer.DIM_WIDTH,
                    JetpackConfig.Items.PRESENT_WIDTH)
        }

        i = 0
        val coalDrawables = intArrayOf(R.drawable.jetpack_coal)
        texItemCoal = IntArray(coalDrawables.size)
        val texItemCoalSnapshot = texItemCoal ?: throw IllegalStateException()
        for (resId in coalDrawables) {
            texItemCoalSnapshot[i++] = renderer.requestImageTex(
                    resId, "small", Renderer.DIM_WIDTH, JetpackConfig.Items.SMALL_WIDTH)
        }

        texCloud = renderer.requestImageTex(
                R.drawable.jetpack_cloud,
                "jetpack_cloud",
                Renderer.DIM_WIDTH,
                JetpackConfig.Clouds.WIDTH)

        texBackground = renderer.requestImageTex(
                backgroundFromCurrentTime,
                "jetpack_background",
                Renderer.DIM_WIDTH,
                renderer.width)

        comboTex = IntArray(3).apply {
            this[0] = renderer.requestImageTex(
                R.drawable.jetpack_combo_2x,
                "jetpack_combo_2x",
                Renderer.DIM_WIDTH,
                JetpackConfig.ComboPopup.SIZE)
            this[1] = renderer.requestImageTex(
                R.drawable.jetpack_combo_3x,
                "jetpack_combo_3x",
                Renderer.DIM_WIDTH,
                JetpackConfig.ComboPopup.SIZE)
            this[2] = renderer.requestImageTex(
                R.drawable.jetpack_combo_4x,
                "jetpack_combo_4x",
                Renderer.DIM_WIDTH,
                JetpackConfig.ComboPopup.SIZE)
        }

        texFire = renderer.requestImageTex(
                R.drawable.jetpack_fire,
                "jetpack_fire",
                Renderer.DIM_WIDTH,
                JetpackConfig.Player.Fire.WIDTH)
        texPlayerHit = renderer.requestImageTex(
                R.drawable.jetpack_player_hit,
                "jetpack_player_hit",
                Renderer.DIM_WIDTH,
                JetpackConfig.Player.WIDTH)
        texPlayerHitOverlay = renderer.requestImageTex(
                R.drawable.jetpack_player_hit_overlay,
                "jetpack_player_hit_overlay",
                Renderer.DIM_WIDTH,
                JetpackConfig.Player.WIDTH)
    }

    companion object {

        // item subtypes
        const val ITEM_PRESENT = 0
        const val ITEM_CANDY = 1
        const val ITEM_COAL = 2
    }
}
