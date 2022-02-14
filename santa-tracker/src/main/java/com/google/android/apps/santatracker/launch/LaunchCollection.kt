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

package com.google.android.apps.santatracker.launch

import java.util.HashMap

class LaunchCollection : Iterable<AbstractLaunch> {

    private val map: HashMap<Int, AbstractLaunch> = HashMap()
    private val sortedList: MutableList<AbstractLaunch> = ArrayList()
    private var cachedVisibleList: List<AbstractLaunch> = ArrayList()

    val numVisibleLaunchers: Int
        get() = cachedVisibleList.size

    override fun iterator(): Iterator<AbstractLaunch> {
        return sortedList.iterator()
    }

    fun add(cardId: Int, launcher: AbstractLaunch) {
        map[cardId] = launcher
        sortedList.add(launcher)
        sortedList.sort()
    }

    fun getLauncherFromCardKey(cardId: Int): AbstractLaunch {
        return map[cardId] ?: throw IllegalArgumentException("Card ID invalid")
    }

    fun getVisibleLauncherFromPosition(position: Int): AbstractLaunch {
        return cachedVisibleList.getOrNull(position) ?: throw IllegalArgumentException(
                "Launcher position $position out of bounds (size: ${cachedVisibleList.size}).")
    }

    fun getReadyGames() = sortedList.filter {
        it.isGame && it.isReady
    }

    fun updateVisibleList() {
        sortedList.sort()
        cachedVisibleList = sortedList.filter { it.state != AbstractLaunch.STATE_HIDDEN }
    }

    fun getPositionFromCardKey(cardId: Int) = cachedVisibleList.indexOf(map[cardId])
}
