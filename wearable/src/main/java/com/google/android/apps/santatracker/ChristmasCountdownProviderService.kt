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

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.support.wearable.complications.ComplicationText.TimeDifferenceBuilder
import android.util.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Provider for a complication that shows the time remaining until Christmas. On Christmas day,
 * the complication will show Santa as icon with no date. On 26th December, the complication will
 * update to show countdown to the next Christmas.
 *
 * By default:
 * + this complication data provider is not set to refreshed until Christmas day, or
 * + if it is Christmas day, it will set to refresh on Dec 26 to countdown to the next Christmas
 * Both of these are done via JobScheduler with a maximum of a one second delay from midnight.
 */
class ChristmasCountdownProviderService : ComplicationProviderService() {

    // private constant use for tagging
    private val tag = "ChristmasCountdown"

    // Job scheduler number for updating the complication
    private val countdownJobId = 1001

    // Maximum time beyond scheduled time for the countdown to be refresh
    private val maxJobLatency = TimeUnit.SECONDS.toMillis(1)

    // Called by the system to update the complication
    override fun onComplicationUpdate(
        complicationId: Int,
        complicationType: Int,
        complicationManager: ComplicationManager
    ) {

        // Check complication type
        if (complicationType != ComplicationData.TYPE_SHORT_TEXT &&
                complicationType != ComplicationData.TYPE_LONG_TEXT) {
            Log.w(tag, "Unexpected type: " + complicationType)
            return
        }

        // Work out whether it is Christmas Day and when's the next Christmas
        var isChristmasDay = false

        val currentTime = Calendar.getInstance()
        val nextChristmas = Calendar.getInstance()

        // Initialise Christmas calendar object by clearing the time
        nextChristmas.clear()
        // Set calendar object to Christmas this year - Month is 11 because it is zero-indexed
        nextChristmas.set(currentTime.get(Calendar.YEAR), 11, 25)

        // Find out if we are in December and then find out if we are:
        //    1. After this year's Christmas, then we should set next Christmas to next year
        //    2. If it is Christmas, set isChristmasDay to true
        // This will drive how the display will be shown later
        if (currentTime.get(Calendar.MONTH) == Calendar.DECEMBER) {

            if (currentTime.get(Calendar.DAY_OF_MONTH) > 25) {
                // Set the next Christmas to next year
                nextChristmas.add(Calendar.YEAR, 1)
            } else if (currentTime.get(Calendar.DAY_OF_MONTH) == 25) {
                // It's Christmas day!
                isChristmasDay = true
            }
        }

        // Debug code
        Log.d(tag, "Current Date: YY MM DD " + currentTime.get(Calendar.YEAR) + " " +
                currentTime.get(Calendar.MONTH) + " " + currentTime.get(Calendar.DAY_OF_MONTH))

        Log.d(tag, "Next Christmas Date: YY MM DD" + nextChristmas.get(Calendar.YEAR) + " " +
                nextChristmas.get(Calendar.MONTH) + " " + nextChristmas.get(Calendar.DAY_OF_MONTH))

        // Initialising countdown text and icon object
        val countdownText: ComplicationText
        val icon: Icon

        if (isChristmasDay) {
            // If it is Christmas day, we will
            //    1. show special greetings
            countdownText =
                    if (complicationType == ComplicationData.TYPE_SHORT_TEXT) {
                        ComplicationText.plainText(
                                resources.getString(R.string.short_christmas_greeting))
                    } else {
                        ComplicationText.plainText(
                                resources.getString(R.string.long_christmas_greeting))
                    }
            //   2. set the icon to Santa
            icon = Icon.createWithResource(this, R.drawable.ic_santaicon)
        } else {
            // If it is not Christmas day, we will
            //    1. show the countdown text on the watch face
            countdownText = TimeDifferenceBuilder()
                    .setReferencePeriodStart(nextChristmas.timeInMillis)
                    .setReferencePeriodEnd(java.lang.Long.MAX_VALUE)
                    .setStyle(
                            if (complicationType == ComplicationData.TYPE_SHORT_TEXT)
                                ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT
                            else
                                ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT)
                    .setShowNowText(false)
                    .build()
            //    2. Set the icon to a wrapped gift box
            icon = Icon.createWithResource(this, R.drawable.ic_gift)
        }

        // Generate the correct Complication data depending on what the watch face demands
        // Short Text or Long Text
        when (complicationType) {
            ComplicationData.TYPE_SHORT_TEXT ->
                complicationManager.updateComplicationData(complicationId,
                        ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                .setIcon(icon)
                                .setShortText(countdownText)
                                .build())
            ComplicationData.TYPE_LONG_TEXT ->
                complicationManager.updateComplicationData(complicationId,
                        ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                .setIcon(icon)
                                .setLongText(countdownText)
                                .build())
            else -> throw IllegalArgumentException("Type must be SHORT TEXT or LONG TEXT")
        }

        // Since we set update period to 0 in AndroidManifest.xml, it means the system may never
        // request a refresh. However, there are two times in the year we would like to update:
        //    1. Midnight Christmas Day, so that we can show the special icon and greetings instead
        //       of the countdown
        //    2. Midnight 26 Dec, so that we can show the countdown again but for next year
        // In these two scenarios, we will want to tickle the system to update the complication.

        // Work out the delay of time in millis before we should refresh the complication
        val delay: Long

        delay =
                if (!isChristmasDay)
                // if this is not Christmas day - we just set the delay to be between the next
                // Christmas and now, when it refresh, it will be Christmas and special greeting
                // is shown
                    nextChristmas.timeInMillis - currentTime.timeInMillis
                else
                // If this is Christmas day - set a refresh for midnight 26 Dec to begin countdown
                // for next year's Christmas
                    nextChristmas.timeInMillis +
                            TimeUnit.DAYS.toMillis(1) - currentTime.timeInMillis

        // Debug line
        Log.d(tag, "delay " + delay)

        // Requesting update
        val jobScheduler = this.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        // Cancel any previous requests for update
        jobScheduler.cancel(countdownJobId)

        jobScheduler.schedule(
                JobInfo.Builder(countdownJobId,
                        ComponentName(this, UpdateCountdownService::class.java))
                        .setMinimumLatency(delay)
                        .setOverrideDeadline(delay + maxJobLatency)
                        .build())
    }
}