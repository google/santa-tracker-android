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

package com.google.android.apps.santatracker.tracker.ui

class PresentCounter(
    private val initialPresents: Long,
    totalPresents: Long,
    private val startTime: Long,
    endTime: Long
) {

    private val presentsDelivery: Long = totalPresents - initialPresents
    private val duration: Double = (endTime - startTime).toDouble()

    fun getDeliveredPresents(time: Long): Long {
        val progress = (time - startTime).toDouble() / duration
        return when {
            progress < 0.0 ->
                // do not return negative presents if progress is incorrect
                initialPresents
            progress > 1.0 -> initialPresents + presentsDelivery
            else -> Math.round(initialPresents + presentsDelivery * progress)
        }
    }
}
