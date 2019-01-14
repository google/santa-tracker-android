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

package com.google.android.apps.santatracker.messaging

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.apps.santatracker.Intents
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.enqueueRefreshAppIndex
import com.google.android.apps.santatracker.util.SantaLog
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * FCM Service to receive data messages and kick off appropriate actions.
 */
class SantaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var config: Config

    private lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        SantaLog.d(TAG, "onMessageReceived:$remoteMessage")
        if (remoteMessage == null) {
            return
        }

        val action = remoteMessage.data[KEY_ACTION]
        if (action == null) {
            SantaLog.w(TAG, "Message has no action, doing nothing.")
            return
        }

        SantaLog.d(TAG, "onMessageReceived:action:$action")

        if (action == ACTION_SYNC_ALL || action == ACTION_SYNC_CONFIG) {
            syncConfig()
        }

        if (action == ACTION_SYNC_ALL || action == ACTION_SYNC_ROUTE) {
            syncRoute()
        }

        if (action == ACTION_SYNC_ALL || action == ACTION_SYNC_STICKERS) {
            enqueueRefreshAppIndex(this)
        }
    }

    override fun onNewToken(token: String?) {
        SantaLog.d(TAG, "New instance token: $token")
    }

    /**
     * Kick off an asynchronous config load.
     */
    private fun syncConfig() {
        SantaLog.d(TAG, "Syncing config...")

        config.syncConfigAsync(object : Config.ParamChangedCallback {
            override fun onChanged(changedKeys: List<String>) {
                SantaLog.d(TAG, "Changed keys from the previous values: $changedKeys")
                localBroadcastManager.sendBroadcast(Intent(Intents.SYNC_CONFIG_INTENT))

                if (Config.ALL_PARAMS_TRACKER.any { it.key in changedKeys }) {
                    SantaLog.d(TAG, "Sending an Intent to finish the tracker")
                    localBroadcastManager.sendBroadcast(Intent(Intents.FINISH_TRACKER_INTENT))
                }
            }
        })
    }

    /**
     * Kick off an asynchronous route load.
     */
    private fun syncRoute() {
        SantaLog.d(TAG, "Syncing route...")

        // TODO: Make sure it works when the app isn't launched.
        //       If FCM message isn't queued when the app isn't launched,
        //       we need to consider some versioning for JSON files
        //       (such as version number in JSON file or compare the file's timestamp)
        localBroadcastManager.sendBroadcast(Intent(Intents.SYNC_ROUTE_INTENT))
        localBroadcastManager.sendBroadcast(Intent(Intents.FINISH_TRACKER_INTENT))
    }

    companion object {
        const val TAG = "MessagingService"

        const val TOPIC_SYNC = "sync"

        const val KEY_ACTION = "action"

        const val ACTION_SYNC_ALL = "sync_all"
        const val ACTION_SYNC_CONFIG = "sync_config"
        const val ACTION_SYNC_ROUTE = "sync_route"
        const val ACTION_SYNC_STICKERS = "sync_stickers"
    }
}
