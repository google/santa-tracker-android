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

package com.google.android.apps.santatracker.invites

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.apps.santatracker.common.R
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.appinvite.AppInvite
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.appinvite.AppInviteReferral
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.analytics.FirebaseAnalytics

class AppInvitesFragment : Fragment(), GoogleApiClient.OnConnectionFailedListener {

    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    interface GetInvitationCallback {
        fun onInvitation(invitationId: String, deepLink: String)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = activity ?: return

        // Initialize app measurement.
        firebaseAnalytics = FirebaseAnalytics.getInstance(activity)

        // Api client for AppInvites.
        googleApiClient = GoogleApiClient.Builder(activity)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(activity, AUTOMANAGE_ID, this)
                .addApi(AppInvite.API)
                .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_INVITE && data != null) {
            val ids = AppInviteInvitation.getInvitationIds(resultCode, data)
            SantaLog.d(TAG, "onActivityResult:$ids")
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        SantaLog.w(TAG, "onConnectionFailed:$connectionResult")
    }

    /**
     * Send an invite with a deep link into a game.
     *
     * @param gameName a human-readable name for the game, to be displayed in the invitation UI.
     * @param gameId an identifier for the game to be appended to
     * http://google.com/santatracker/android/. The game should be a registered handler for this
     * URL in the Android Manifest.
     * @param score the inviting user's game score, which will be pre-populated in the invitation
     * message.
     */
    fun sendGameInvite(gameName: String, gameId: String, score: Int) {
        val uri = BASE_URI.buildUpon()
                .appendPath(gameId)
                .appendQueryParameter("score", Integer.toString(score))
                .build()

        sendInvite(getString(R.string.invite_message_game_fmt, score, gameName), uri)
        MeasurementManager.recordInvitationSent(firebaseAnalytics, "game", uri.toString())
    }

    fun sendGenericInvite() {
        val uri = BASE_URI
        sendInvite(getString(R.string.invite_message_generic), uri)
        MeasurementManager.recordInvitationSent(firebaseAnalytics, "generic", uri.toString())
    }

    private fun sendInvite(message: String, uri: Uri) {
        var message = message
        // If the message is too long, just cut it short and add ellipses. This is something that
        // only occurs in some translations and we do not have a better mitigation method.  The
        // alternative is an ugly IllegalArgumentException from the builder.
        val maxLength = AppInviteInvitation.IntentBuilder.MAX_MESSAGE_LENGTH
        if (message.length > maxLength) {
            val suffix = "..."
            val prefix = message.substring(0, maxLength - suffix.length)

            message = prefix + suffix
        }

        val inviteIntent = AppInviteInvitation.IntentBuilder(getString(R.string.invite_title))
                .setMessage(message)
                .setDeepLink(uri)
                .build()

        startActivityForResult(inviteIntent, RC_INVITE)
    }

    fun getInvite(callback: GetInvitationCallback?, launchDeepLink: Boolean) {
        // Using "null, false" as arguments here to avoid a known memory leak issue in
        // AppInvites. Should be fixed in Google Play services v10.4.0.
        val activity = activity
        AppInvite.AppInviteApi.getInvitation(googleApiClient, null, false)
                .setResultCallback { appInviteInvitationResult ->
                    SantaLog.d(TAG, "getInvite:" + appInviteInvitationResult.status)

                    if (callback != null && appInviteInvitationResult.status.isSuccess) {
                        // Report the callback.
                        val intent = appInviteInvitationResult.invitationIntent
                        val invitiationId = AppInviteReferral.getInvitationId(intent)
                        val deepLink = AppInviteReferral.getDeepLink(intent)
                        callback.onInvitation(invitiationId, deepLink)

                        // Record invitation receipt event.
                        MeasurementManager.recordInvitationReceived(
                                firebaseAnalytics, deepLink)

                        // Launch the deep link (see above note on why we don't do this
                        // automatically)
                        activity?.let {
                            if (launchDeepLink) {
                                try {
                                    it.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    SantaLog.w(TAG, "No handler for deep link", e)
                                }
                            }
                        }
                    }
                }
    }

    companion object {

        private const val TAG = "AppInvitesFragment"
        private const val FRAGMENT_TAG = "AppInvitesFragment"
        private const val AUTOMANAGE_ID = 107
        private const val RC_INVITE = 9007

        val BASE_URI: Uri = Uri.parse("https://google.com/santatracker/android/")

        @JvmStatic
        fun getInstance(activity: FragmentActivity): AppInvitesFragment? {

            val fm = activity.supportFragmentManager
            val ft = fm.beginTransaction()

            var result: AppInvitesFragment?
            val fragment = fm.findFragmentByTag(FRAGMENT_TAG)
            if (fragment == null) {
                result = AppInvitesFragment()
                ft.add(result, FRAGMENT_TAG).disallowAddToBackStack().commit()
            } else {
                result = fragment as AppInvitesFragment?
            }
            return result
        }
    }
}
