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

package com.google.android.apps.santatracker

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.google.android.apps.santatracker.stickers.SantaTrackerStickers
import dagger.android.AndroidInjection
import javax.inject.Inject

class AppIndexingUpdateService : JobIntentService() {
    @Inject lateinit var santaTrackerStickers: SantaTrackerStickers

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onHandleWork(intent: Intent) {
        santaTrackerStickers.updateStickers()
    }

    companion object {
        internal const val UNIQUE_JOB_ID = 89
    }
}

fun enqueueRefreshAppIndex(context: Context) {
    JobIntentService.enqueueWork(
            context,
            AppIndexingUpdateService::class.java,
            AppIndexingUpdateService.UNIQUE_JOB_ID,
            Intent()
    )
}