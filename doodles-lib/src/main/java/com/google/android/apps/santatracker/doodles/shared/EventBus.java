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
package com.google.android.apps.santatracker.doodles.shared;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A simple event bus for passing events between objects. */
public class EventBus {
    public static final int VIBRATE = 0;
    public static final int SCORE_CHANGED = 1;
    public static final int SHAKE_SCREEN = 2;
    public static final int BRONZE = 3;
    public static final int SILVER = 4;
    public static final int GOLD = 5;
    public static final int SWIMMING_DIVE = 6;
    public static final int GAME_STATE_CHANGED = 7;
    public static final int PLAY_SOUND = 8;
    public static final int PAUSE_SOUND = 9;
    public static final int MUTE_SOUNDS = 10;
    public static final int GAME_OVER = 11;
    public static final int GAME_LOADED = 12;
    private static EventBus instance;
    private final Object lock = new Object();
    // Listeners for specific events.
    private Map<Integer, Set<EventBusListener>> specificListeners;
    // Listeners for all events.
    private Set<EventBusListener> globalListeners;

    private EventBus() {
        globalListeners = new HashSet<>();
        specificListeners = new HashMap<>();
    }

    public static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    /** Register for a specific event. Listener will only be called for events of that type. */
    public void register(EventBusListener listener, int type) {
        synchronized (lock) {
            if (!specificListeners.containsKey(type)) {
                specificListeners.put(type, new HashSet<EventBusListener>());
            }
            specificListeners.get(type).add(listener);
        }
    }

    /** Register for all events. Listener will be called for events of any type. */
    public void register(EventBusListener listener) {
        synchronized (lock) {
            globalListeners.add(listener);
        }
    }

    /** Send an event without data. */
    public void sendEvent(int type) {
        sendEvent(type, null);
    }

    /** Send an event with data. Type of the data is up to the caller. */
    public void sendEvent(int type, Object data) {
        synchronized (lock) {
            try {
                Set<EventBusListener> listeners = specificListeners.get(type);
                if (listeners != null) {
                    for (EventBusListener listener : listeners) {
                        listener.onEventReceived(type, data);
                    }
                }
                for (EventBusListener listener : globalListeners) {
                    listener.onEventReceived(type, data);
                }
            } catch (ClassCastException e) {
                // This was happening when 2 games were running at the same time (which shouldn't be
                // possible, but was happening in monkey testing). Game A's listener would try
                // casting
                // the data arg to the expected type for Game A, but this would fail if Game B sent
                // a data
                // of a different type.
                //
                // Ignore this and continue running.
            }
        }
    }

    /** Removes all the listeners from this EventBus. */
    public void clearListeners() {
        synchronized (lock) {
            specificListeners.clear();
            globalListeners.clear();
        }
    }

    /** Interface for objects which want to listen to the event bus. */
    public interface EventBusListener {
        void onEventReceived(int type, Object data);
    }
}
