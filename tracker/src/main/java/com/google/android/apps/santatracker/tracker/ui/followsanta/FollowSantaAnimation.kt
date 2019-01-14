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

package com.google.android.apps.santatracker.tracker.ui.followsanta

import android.os.Handler
import com.google.android.apps.santatracker.tracker.ui.SantaMarker
import com.google.android.gms.maps.GoogleMap

/**
 * An animation executed during Santa Cam mode.
 */
abstract class FollowSantaAnimation(
    private val handler: Handler,
    val googleMap: GoogleMap,
    val santaMarker: SantaMarker
) {
    var isCancelled: Boolean = false

    open fun reset() {
        isCancelled = false
    }

    fun cancel() {
        // stop execution
        isCancelled = true
    }

    fun executeRunnable(r: Runnable) {
        if (!isCancelled) {
            handler.post(r)
        }
    }
}
