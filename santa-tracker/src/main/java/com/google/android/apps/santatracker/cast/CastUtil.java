/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.cast;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.android.apps.santatracker.util.PlayServicesUtil;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;

/**
 * Utility methods for Cast SDK v3 integration.
 */
public class CastUtil {

    /**
     * Ends the current cast session.
     *
     * @see SessionManager#endCurrentSession(boolean)
     */
    public static void stopCasting(Context context) {
        SessionManager sessionManager = getSessionManager(context);
        if (sessionManager == null) {
            return;
        }

        // Stop casting by ending the current CastSession.
        sessionManager.endCurrentSession(true);
    }

    /**
     * Returns true if a cast session is currently active and connected.
     *
     * @return True if a cast session is connected.
     * @see CastSession#isConnected()
     */
    public static boolean isCasting(Context context) {

        SessionManager sessionManager = getSessionManager(context);
        if (sessionManager == null) {
            return false;
        }

        CastSession castSession = sessionManager.getCurrentCastSession();
        return castSession != null && castSession.isConnected();
    }

    /**
     * Registers a {@link SessionManagerListener} with the {@link SessionManager}.
     *
     * @see SessionManager#addSessionManagerListener(SessionManagerListener)
     */
    public static void registerCastListener(Context context, SessionManagerListener listener) {
        SessionManager sessionManager = getSessionManager(context);
        if (sessionManager == null) {
            return;
        }
        sessionManager.addSessionManagerListener(listener);
    }

    /**
     * Removes a {@link SessionManagerListener} with the {@link SessionManager}.
     *
     * @see SessionManager#removeSessionManagerListener(SessionManagerListener, Class)
     */
    public static void removeCastListener(Context context, SessionManagerListener listener) {
        SessionManager sessionManager = getSessionManager(context);
        if (sessionManager == null) {
            return;
        }
        sessionManager.removeSessionManagerListener(listener);
    }

    /**
     * Registers a {@link com.google.android.gms.cast.framework.CastState} with the
     * {@link SessionManager}.
     *
     * @see SessionManager#addCastStateListener(CastStateListener)
     */
    public static void registerCastStateListener(Context context, CastStateListener listener) {
        SessionManager sessionManager = getSessionManager(context);
        if (sessionManager == null) {
            return;
        }
        sessionManager.addCastStateListener(listener);
    }

    /**
     * Removes a {@link CastStateListener} with the {@link SessionManager}.
     *
     * @see SessionManager#removeSessionManagerListener(SessionManagerListener, Class)
     */
    public static void removeCastStateListener(Context context, CastStateListener listener) {
        SessionManager sessionManager = getSessionManager(context);
        if (sessionManager == null) {
            return;
        }
        sessionManager.removeCastStateListener(listener);
    }

    /**
     * Returns the {@link SessionManager} from the current {@link CastContext}.
     *
     * @return The current CastContext or null if the CastContext or SessionManager are not active.
     */
    @Nullable
    private static SessionManager getSessionManager(Context context) {
        // Do not try to talk to the Cast SDK if we don't have the right version of Play Services,
        // otherwise there could be a fatal exception.
        if (!PlayServicesUtil.hasPlayServices(context)) {
            return null;
        }

        CastContext castContext = CastContext.getSharedInstance(context);
        if (castContext == null) {
            return null;
        }

        return castContext.getSessionManager();
    }

}
