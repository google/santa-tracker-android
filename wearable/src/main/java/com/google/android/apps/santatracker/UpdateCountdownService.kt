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

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.support.wearable.complications.ProviderUpdateRequester
import android.util.Log

/**
 * Update countdown service tickles the ChristmasCountdownProviderService and ask it to refresh
 * the complication text
 */

class UpdateCountdownService : JobService() {

    // private constant used for logging
    private val tag = "UpdateCountdownService"

    override fun onStartJob(jobParameters: JobParameters): Boolean {

        // set the complication service to be refreshed
        val providerService = ComponentName(this,
                "com.google.android.apps.santatracker.ChristmasCountdownProviderService")

        // request the complication service to run the complication service again
        val requester = ProviderUpdateRequester(this, providerService)
        requester.requestUpdateAll()

        Log.d(tag, "Update requested")
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }
}
