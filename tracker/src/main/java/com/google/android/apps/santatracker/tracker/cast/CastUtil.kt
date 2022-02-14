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

package com.google.android.apps.santatracker.tracker.cast

import android.content.Context

import com.google.android.apps.santatracker.util.PlayServicesUtil
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener

/**
 * Utility methods for Cast SDK v3 integration.
 */
object CastUtil {

    /**
     * Ends the current cast session.
     *
     * @see SessionManager.endCurrentSession
     */
    @JvmStatic
    fun stopCasting(context: Context) {
        val sessionManager = getSessionManager(context) ?: return

        // Stop casting by ending the current CastSession.
        sessionManager.endCurrentSession(true)
    }

    /**
     * Returns true if a cast session is currently active and connected.
     *
     * @return True if a cast session is connected.
     * @see CastSession.isConnected
     */
    @JvmStatic
    fun isCasting(context: Context): Boolean {

        val sessionManager = getSessionManager(context) ?: return false

        val castSession = sessionManager.currentCastSession
        return castSession != null && castSession.isConnected
    }

    /**
     * Registers a [SessionManagerListener] with the [SessionManager].
     *
     * @see SessionManager.addSessionManagerListener
     */
    @JvmStatic
    fun registerCastListener(context: Context, listener: SessionManagerListener<Session>) {
        val sessionManager = getSessionManager(context) ?: return
        sessionManager.addSessionManagerListener(listener)
    }

    /**
     * Removes a [SessionManagerListener] with the [SessionManager].
     *
     * @see SessionManager.removeSessionManagerListener
     */
    @JvmStatic
    fun removeCastListener(context: Context, listener: SessionManagerListener<Session>) {
        val sessionManager = getSessionManager(context) ?: return
        sessionManager.removeSessionManagerListener(listener)
    }

    /**
     * Registers a [com.google.android.gms.cast.framework.CastState] with the
     * [SessionManager].
     *
     * @see SessionManager.addCastStateListener
     */
    @JvmStatic
    fun registerCastStateListener(context: Context, listener: CastStateListener) {
        val castContext = CastContext.getSharedInstance(context) ?: return
        castContext.addCastStateListener(listener)
    }

    /**
     * Removes a [CastStateListener] with the [SessionManager].
     *
     * @see SessionManager.removeSessionManagerListener
     */
    @JvmStatic
    fun removeCastStateListener(context: Context, listener: CastStateListener) {
        val castContext = CastContext.getSharedInstance(context) ?: return
        castContext.removeCastStateListener(listener)
    }

    /**
     * Returns the [SessionManager] from the current [CastContext].
     *
     * @return The current CastContext or null if the CastContext or SessionManager are not active.
     */
    @JvmStatic
    private fun getSessionManager(context: Context): SessionManager? {
        // Do not try to talk to the Cast SDK if we don't have the right version of Play Services,
        // otherwise there could be a fatal exception.
        if (!PlayServicesUtil.hasPlayServices(context)) {
            return null
        }

        val castContext = CastContext.getSharedInstance(context) ?: return null

        return castContext.sessionManager
    }
}
