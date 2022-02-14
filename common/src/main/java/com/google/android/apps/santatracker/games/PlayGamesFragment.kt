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

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.apps.santatracker.common.BuildConfig
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games

/** Non-visible fragment to encapsulate Google Play Game Services logic.  */
class PlayGamesFragment : Fragment(), GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /** Should debug-level log messages be printed?  */
    private var debugLogEnabled = false

    /** GoogleApiClient used for interacting with Play Game Services.  */
    var gamesApiClient: GoogleApiClient? = null

    /** Is a resolution already in progress?  */
    private var isResolving = false

    /** Should connection failures be automatically resolved?  */
    private var shouldResolve = false

    /** Listener for sign-in events.  */
    private var listener: SignInListener? = null

    val isSignedIn: Boolean
        get() = gamesApiClient?.isConnected == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore state of in-progress sign-in in the case of rotation or other
        // Activity recreation.
        if (savedInstanceState != null) {
            isResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING, false)
            shouldResolve = savedInstanceState.getBoolean(KEY_SHOULD_RESOLVE, false)
        }
    }

    override fun onStart() {
        super.onStart()
        gamesApiClient?.connect()
    }

    override fun onStop() {
        super.onStop()
        gamesApiClient?.disconnect()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Only log debug messages when enabled
        debugLogEnabled = BuildConfig.DEBUG

        val activity = activity ?: return
        // Api client for interacting with Google Play Games
        gamesApiClient = GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi<Games.GamesOptions>(Games.API, Games.GamesOptions.builder().build())
                .addScope(Games.SCOPE_GAMES)
                .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_GAMES) {
            debugLog("onActivityResult:RC_GAMES:$resultCode:$data")

            // If the error resolution was not successful we should not resolve further.
            if (resultCode != Activity.RESULT_OK) {
                shouldResolve = false
            }

            isResolving = false
            gamesApiClient?.connect()
        }
    }

    override fun onConnected(bundle: Bundle?) {
        debugLog("onConnected $bundle")
        shouldResolve = false
        listener?.onSignInSucceeded()
    }

    override fun onConnectionSuspended(i: Int) {
        debugLog("onConnectionSuspended:$i")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        debugLog("onConnectionFailed:$connectionResult")
        if (!isResolving && shouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(activity, RC_GAMES)
                    isResolving = true
                } catch (e: IntentSender.SendIntentException) {
                    debugLog("onConnectionFailed:SendIntentException:" + e.message)
                    isResolving = false
                    gamesApiClient?.connect()
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                showErrorDialog(connectionResult)
            }
        } else {
            // Show the signed-out UI
            listener?.onSignInFailed()
        }
    }

    /**
     * Show error dialog for Google Play Services errors that cannot be resolved.
     *
     * @param connectionResult the connection result from onConnectionFailed.
     */
    private fun showErrorDialog(connectionResult: ConnectionResult) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability
                        .getErrorDialog(
                                activity,
                                resultCode,
                                RC_GAMES
                        ) {
                            shouldResolve = false
                            listener?.onSignInFailed()
                        }
                        .show()
            } else {
                val errorString = apiAvailability.getErrorString(resultCode)
                debugLog("Google Play Services Error:$connectionResult:$errorString")

                shouldResolve = false
                listener?.onSignInFailed()
            }
        }
    }

    fun beginUserInitiatedSignIn() {
        shouldResolve = true
        gamesApiClient?.connect()
    }

    private fun debugLog(message: String) {
        if (!debugLogEnabled) {
            return
        }

        SantaLog.d(TAG, message)
    }

    private fun setListener(listener: SignInListener) {
        this.listener = listener
    }

    companion object {

        private const val TAG = "PlayGamesFragment"
        private const val FRAGMENT_TAG = "PlayGamesFragment_Tag"

        /** Key to store isResolving in SharedPreferences.  */
        private const val KEY_IS_RESOLVING = "is_resolving"

        /** Key to store shouldResolve in SharedPreferences.  */
        private const val KEY_SHOULD_RESOLVE = "should_resolve"

        /** Request code used for resolving Games sign-in failures.  */
        private const val RC_GAMES = 9001

        /**
         * Get or create an instance of the Fragment attached to an Activity.
         *
         * @param activity FragmentActivity to host the Fragment.
         * @param listener SignInListener to respond to changes in sign-in state.
         * @return instance of PlayGamesFragment.
         */
        @JvmStatic
        fun getInstance(
            activity: androidx.fragment.app.FragmentActivity,
            listener: SignInListener
        ): PlayGamesFragment? {

            val fm = activity.supportFragmentManager
            val ft = fm.beginTransaction()

            val result: PlayGamesFragment?
            val fragment = fm.findFragmentByTag(FRAGMENT_TAG)
            if (fragment == null) {
                result = PlayGamesFragment()
                ft.add(result, FRAGMENT_TAG).disallowAddToBackStack().commit()
            } else {
                result = fragment as PlayGamesFragment?
            }

            result?.setListener(listener)
            return result
        }
    }
}
