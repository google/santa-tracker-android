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

package com.google.android.apps.santatracker.games

import android.app.PendingIntent
import android.content.IntentSender
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.CANCELED
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.DOWNLOADING
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.FAILED
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.INSTALLED
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.INSTALLING
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.PENDING
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.UNKNOWN

/**
 * Listener for loading dynamic feature modules on demand.
 */
abstract class FeatureLoadStateListener : SplitInstallStateUpdatedListener {

    private var isRegistered = false

    fun register(manager: SplitInstallManager) {
        manager.registerListener(this)
        isRegistered = true
    }

    fun unregister(manager: SplitInstallManager) {
        if (isRegistered) {
            manager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onStateUpdate(state: SplitInstallSessionState) {
        SantaLog.d(TAG, "onStateUpdate. Status: ${state.status()}")

        when (state.status()) {
            PENDING -> onPending()
            REQUIRES_USER_CONFIRMATION -> requiresConfirmation(state)
            DOWNLOADING -> onDownloading(state.bytesDownloaded(), state.totalBytesToDownload())
            INSTALLING -> onInstalling()
            INSTALLED -> onInstalled()
            UNKNOWN or FAILED -> onFailure()
            CANCELED -> onCanceled()
        }
    }

    open fun onPending() = Unit

    private fun requiresConfirmation(state: SplitInstallSessionState) {
        val resolutionIntent: PendingIntent = state.resolutionIntent() ?: return
        onRequiresConfirmation(resolutionIntent.intentSender)
    }

    abstract fun onRequiresConfirmation(intentSender: IntentSender)

    abstract fun onDownloading(bytesDownloaded: Long, totalBytesToDownload: Long)

    abstract fun onInstalling()

    open fun onInstalled() = Unit

    open fun onFailure() = Unit

    open fun onCanceled() = Unit

    companion object {
        const val TAG = "FeatureLoadStateListener"
    }
}
