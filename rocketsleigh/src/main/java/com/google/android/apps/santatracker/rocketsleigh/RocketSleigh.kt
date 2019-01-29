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

package com.google.android.apps.santatracker.rocketsleigh

import androidx.annotation.DrawableRes
import java.util.Random

data class Obstacle(
    @DrawableRes
    val top: Int,
    @DrawableRes
    val bottom: Int,
    @DrawableRes
    val back: Int = 0
)

data class Level(
    @DrawableRes
    val background: Int,
    @DrawableRes
    val foreground: Int,
    @DrawableRes
    val entryTransition: Int,
    @DrawableRes
    val exitTransition: Int,
    val obstacles: List<Obstacle>
)

object RocketSleigh {

    val elf = intArrayOf(
            R.drawable.img_jetelf_100,
            R.drawable.img_jetelf_75,
            R.drawable.img_jetelf_50,
            R.drawable.img_jetelf_25,
            R.drawable.img_jetelf_0)

    val elfHit = intArrayOf(
            R.drawable.img_jetelf_100_hit,
            R.drawable.img_jetelf_75_hit,
            R.drawable.img_jetelf_50_hit,
            R.drawable.img_jetelf_25_hit)

    val elfBurn = intArrayOf(
            R.drawable.img_jet_burn_100,
            R.drawable.img_jet_burn_75,
            R.drawable.img_jet_burn_50,
            R.drawable.img_jet_burn_25)

    val elfThrust = intArrayOf(
            R.drawable.img_jet_thrust_100,
            R.drawable.img_jet_thrust_75,
            R.drawable.img_jet_thrust_50,
            R.drawable.img_jet_thrust_25)

    val elfSmoke = intArrayOf(
            R.drawable.img_jet_smoke_100_hit,
            R.drawable.img_jet_smoke_75_hit,
            R.drawable.img_jet_smoke_50_hit,
            R.drawable.img_jet_smoke_25_hit)

    fun generateLevels(random: Random): List<Level> {
        val woods = shuffle(woodsObstacles, 200, random)
        val cave = shuffle(caveObstacles, 200, random)
        val factory = shuffle(factoryObstacles, 200, random)
        return listOf(
                Level(
                        background = R.drawable.bg_jet_pack_1,
                        foreground = R.drawable.img_snow_ground_tiles,
                        entryTransition = 0,
                        exitTransition = 0,
                        obstacles = woods),
                Level(
                        background = R.drawable.bg_jet_pack_2,
                        foreground = R.drawable.img_snow_ground_tiles,
                        entryTransition = 0,
                        exitTransition = 0,
                        obstacles = woods),
                Level(
                        background = R.drawable.bg_jet_pack_3,
                        foreground = R.drawable.img_snow_ground_tiles,
                        entryTransition = R.drawable.bg_transition_1,
                        exitTransition = 0,
                        obstacles = cave),
                Level(
                        background = R.drawable.bg_jet_pack_4,
                        foreground = R.drawable.img_snow_ground_tiles,
                        entryTransition = 0,
                        exitTransition = R.drawable.bg_transition_2,
                        obstacles = cave),
                Level(
                        background = R.drawable.bg_jet_pack_5,
                        foreground = 0,
                        entryTransition = R.drawable.bg_transition_3,
                        exitTransition = 0,
                        obstacles = factory),
                Level(
                        background = R.drawable.bg_jet_pack_6,
                        foreground = 0,
                        entryTransition = 0,
                        exitTransition = R.drawable.bg_transition_4,
                        obstacles = factory)
        )
    }

    private fun <E> shuffle(source: List<E>, length: Int, random: Random): List<E> {
        val list = mutableListOf<E>()
        for (i in 0 until length) {
            list.add(source[random.nextInt(source.size)])
        }
        return list
    }

    fun randomGift(random: Random) = giftBoxes.get(random.nextInt(giftBoxes.size))

    val giftBoxes = intArrayOf(
            R.drawable.img_gift_blue_jp,
            R.drawable.img_gift_green_jp,
            R.drawable.img_gift_yellow_jp,
            R.drawable.img_gift_purple_jp,
            R.drawable.img_gift_red_jp
    )

    val woodsObstacles = listOf(
            Obstacle(0, R.drawable.img_pine_1_bottom, R.drawable.img_pine_0),
            Obstacle(0, R.drawable.img_pine_2_bottom, R.drawable.img_pine_0),
            Obstacle(0, R.drawable.img_pine_3_bottom, R.drawable.img_pine_0),
            Obstacle(0, R.drawable.img_pine_4_bottom, R.drawable.img_pine_0),
            Obstacle(R.drawable.img_pine_1_top, 0, R.drawable.img_pine_0),
            Obstacle(R.drawable.img_pine_2_top, 0, R.drawable.img_pine_0),
            Obstacle(R.drawable.img_pine_3_top, 0, R.drawable.img_pine_0),
            Obstacle(R.drawable.img_pine_4_top, 0, R.drawable.img_pine_0),
            Obstacle(0, R.drawable.img_birch_1_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_birch_2_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_birch_3_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_birch_4_bottom, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_birch_1_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_birch_2_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_birch_3_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_birch_4_top, 0, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_tree_1_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_tree_2_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_tree_3_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_tree_4_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_tree_5_bottom, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_tree_6_bottom, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_tree_1_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_tree_2_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_tree_3_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_tree_4_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_tree_5_top, 0, R.drawable.img_birch_0),
            Obstacle(R.drawable.img_tree_6_top, 0, R.drawable.img_birch_0),
            Obstacle(0, R.drawable.img_owl, 0),
            Obstacle(0, R.drawable.img_log_elf, 0),
            Obstacle(0, R.drawable.img_bear_big, 0),
            Obstacle(0, R.drawable.img_bear_little, 0))

    val caveObstacles = listOf(
            Obstacle(R.drawable.img_icicle_small_3, 0),
            Obstacle(R.drawable.img_icicle_small_4, 0),
            Obstacle(R.drawable.img_icicle_med_3, 0),
            Obstacle(R.drawable.img_icicle_med_4, 0),
            Obstacle(R.drawable.img_icicle_lrg_2, 0),
            Obstacle(0, R.drawable.img_icicle_small_1),
            Obstacle(0, R.drawable.img_icicle_small_2),
            Obstacle(0, R.drawable.img_icicle_med_1),
            Obstacle(0, R.drawable.img_icicle_med_2),
            Obstacle(0, R.drawable.img_icicle_lrg_1),
            Obstacle(R.drawable.img_icicle_small_3, R.drawable.img_icicle_small_1),
            Obstacle(R.drawable.img_icicle_small_3, R.drawable.img_icicle_small_2),
            Obstacle(R.drawable.img_icicle_small_4, R.drawable.img_icicle_small_1),
            Obstacle(R.drawable.img_icicle_small_4, R.drawable.img_icicle_small_2),
            Obstacle(R.drawable.img_2_bats, 0),
            Obstacle(R.drawable.img_3_bats, 0),
            Obstacle(R.drawable.img_4_bats, 0),
            Obstacle(R.drawable.img_5_bats, 0),
            Obstacle(0, R.drawable.img_yeti),
            Obstacle(0, R.drawable.img_mammoth),
            Obstacle(0, R.drawable.img_snow_kiss),
            Obstacle(0, R.drawable.img_snowman))

    val factoryObstacles = listOf(
            Obstacle(R.drawable.img_icecream_drop, R.drawable.img_icecream_0),
            Obstacle(R.drawable.img_icecream_drop, R.drawable.img_icecream_1),
            Obstacle(R.drawable.img_mint_drop_top, R.drawable.img_mint_drop_bottom),
            Obstacle(R.drawable.img_mint_stack_top, R.drawable.img_mint_stack_bottom),
            Obstacle(0, R.drawable.img_candy_cane_0),
            Obstacle(R.drawable.img_candy_cane_1, 0),
            Obstacle(0, R.drawable.img_lollipops),
            Obstacle(0, R.drawable.img_choco_fountn),
            Obstacle(0, R.drawable.img_candy_buttons),
            Obstacle(0, R.drawable.img_mint_gondola),
            Obstacle(0, R.drawable.img_candy_cane_0),
            Obstacle(R.drawable.img_candy_cane_1, 0),
            Obstacle(0, R.drawable.img_lollipops),
            Obstacle(0, R.drawable.img_choco_fountn),
            Obstacle(0, R.drawable.img_candy_buttons),
            Obstacle(0, R.drawable.img_mint_gondola),
            Obstacle(0, R.drawable.img_candy_cane_0),
            Obstacle(R.drawable.img_candy_cane_1, 0),
            Obstacle(0, R.drawable.img_lollipops),
            Obstacle(0, R.drawable.img_choco_fountn),
            Obstacle(0, R.drawable.img_candy_buttons),
            Obstacle(0, R.drawable.img_mint_gondola))
}
