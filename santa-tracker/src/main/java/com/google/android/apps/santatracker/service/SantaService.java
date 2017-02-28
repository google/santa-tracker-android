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

package com.google.android.apps.santatracker.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.DestinationDbHelper;
import com.google.android.apps.santatracker.data.GameDisabledState;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.data.StreamDbHelper;
import com.google.android.apps.santatracker.util.SantaLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class SantaService extends Service implements APIProcessor.APICallback {

    private static final String TAG = "SantaCommunicator";

    private static final String FILENAME_OVERRIDE = "santa_config.txt";

    // Parameters from config resources
    private String API_URL;
    private String API_CLIENT;
    private String LANGUAGE;
    public int INITIAL_BACKOFF_TIME;
    public int MAX_BACKOFF_TIME;
    public float BACKOFF_FACTOR;
    private static final int TIMEZONE = TimeZone.getDefault().getRawOffset();

    private long mBackoff;

    private SantaPreferences mPreferences;
    private DestinationDbHelper mDbHelper;

    // current state of the service
    private int mState = SantaServiceMessages.STATUS_IDLE_NODATA;

    private ArrayList<Messenger> mClients = new ArrayList<>(2);
    private final ArrayList<Messenger> mPendingClients = new ArrayList<>(2);

    private Messenger mIncomingMessenger;

    private APIProcessor mApiProcessor;

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 4, 60L,
            TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

    private Handler mHandler = null;
    private HandlerThread mApiThread = new HandlerThread("ApiThread");

    private Runnable mApiRunnable = new Runnable() {
        @Override
        public void run() {
            // Prevent clients from being added during thread execution
            synchronized (mPendingClients) {

                // Sanity check to ensure that next access timestamp has been met
                if (System.currentTimeMillis() < mPreferences.getNextInfoAPIAccess()) {
                    SantaLog.d(TAG, "Did not run API thread, next API access not expired: next="
                            + mPreferences.getNextInfoAPIAccess() + " ,current=" + System
                            .currentTimeMillis() +
                            ", diff=" + (mPreferences.getNextInfoAPIAccess() - System
                            .currentTimeMillis()));
                    //reschedule
                    scheduleApiAccess();

                    // skip if no clients are registered
                } else if (!mClients.isEmpty() || !mPendingClients.isEmpty()) {
                    sendPendingState();
                    long delay = accessInfoAPI();
                    sendPendingState();
                    mPreferences.setNextInfoAPIAccess(delay + System.currentTimeMillis());
                    SantaLog.d(TAG, "delay=" + delay + ", next access=" + mPreferences
                            .getNextInfoAPIAccess() + " current time=" + System.currentTimeMillis()
                            + " diff=" + (mPreferences.getNextInfoAPIAccess() - System
                            .currentTimeMillis()));

                    // do not reschedule unless there are clients registered
                    if (!mClients.isEmpty() || !mPendingClients.isEmpty()) {
                        scheduleApiAccess();
                    } else {
                        SantaLog.d(TAG, "No clients registered, access not scheduled.");
                    }
                }
            }

        }
    };


    private void scheduleApiAccess() {
        mHandler.removeCallbacksAndMessages(null);

        final long nextAccess = mPreferences.getNextInfoAPIAccess() - System.currentTimeMillis();
        if (nextAccess <= 0) {
            SantaLog.d(TAG, "schedule: negative, post now.");
            // run straight away
            mHandler.post(mApiRunnable);
        } else {
            SantaLog.d(TAG, "schedule: positive, postDelayed in: " + nextAccess);
            mHandler.postDelayed(mApiRunnable, nextAccess);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mIncomingMessenger = new Messenger(new IncomingHandler(this));
        mState = SantaServiceMessages.STATUS_IDLE_NODATA;
        startHandlerThread();

        // initialise config values
        final Resources res = getResources();
        INITIAL_BACKOFF_TIME = res.getInteger(R.integer.backoff_initital);
        MAX_BACKOFF_TIME = res.getInteger(R.integer.backoff_max);
        BACKOFF_FACTOR = ((float) res.getInteger(R.integer.backoff_factor)) / 100f;
        mBackoff = INITIAL_BACKOFF_TIME;

        LANGUAGE = Locale.getDefault().getLanguage();

        mPreferences = new SantaPreferences(getApplicationContext());
        mDbHelper = DestinationDbHelper.getInstance(getApplicationContext());
        StreamDbHelper streamDbHelper = StreamDbHelper.getInstance(getApplicationContext());

        // invalidate all data if database has been upgraded (or started for the
        // first time)
        if (mPreferences.getDestDBVersion() != DestinationDbHelper.DATABASE_VERSION ||
                mPreferences.getStreamDBVersion() != StreamDbHelper.DATABASE_VERSION) {
            SantaLog.d(TAG, "Data is invalid - reinitialising.");
            mDbHelper.reinitialise();
            streamDbHelper.reinitialise();
            mPreferences.invalidateData();
            mPreferences.setDestDBVersion(DestinationDbHelper.DATABASE_VERSION);
            mPreferences.setStreamDBVersion(StreamDbHelper.DATABASE_VERSION);
        }

        // ensure a valid rand value is stored
        if (mPreferences.getRandValue() < 0) {
            // invalid rand value, generate new value and update preference
            float rand = (float) Math.random();
            mPreferences.setRandValue(rand);
        }

        // Read in the URL and CLIENT values from the sdcard if the file exists, otherwise use
        // defaults from resources
        if (!setOverrideConfigValues()) {
            API_URL = res.getString(R.string.api_url);
            API_CLIENT = res.getString(R.string.config_api_client);
        }

        // Initialise the ApiProcessor. If the client is "local" use the special debug processor for
        // a local file.
        if (API_CLIENT.equals("local")) {
            Toast.makeText(this, "Using Local API file!", Toast.LENGTH_SHORT).show();
            // For a local data file, remove all existing data first when the file is initialised
            mApiProcessor = new LocalApiProcessor(mPreferences, mDbHelper, streamDbHelper, this);
        } else {
            // Default processor that accesses the remote api via HTTPS.
            mApiProcessor = new RemoteApiProcessor(mPreferences, mDbHelper, streamDbHelper, this);
        }

        // Check state of data - is it up to date?
        if (haveValidData()) {
            mState = SantaServiceMessages.STATUS_IDLE;
        } else {
            mState = SantaServiceMessages.STATUS_IDLE_NODATA;
        }

    }

    @Override
    public void onDestroy() {
        mIncomingMessenger = null;
        super.onDestroy();
    }

    /**
     * Attempt to read in a file from external storage with config options.
     * The file needs to be located in {@link android.os.Environment#getExternalStorageDirectory()}
     * and named {@link #FILENAME_OVERRIDE}. It contains one line of text: the client name,
     * followed
     * by the API URL.
     */
    private boolean setOverrideConfigValues() {

        File f = new File(Environment.getExternalStorageDirectory(), FILENAME_OVERRIDE);

        if (f.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = br.readLine();
                br.close();

                // parse the line
                final int commaPosition = line.indexOf(',');
                if (commaPosition > 0) {
                    final String client = line.substring(0, commaPosition);
                    final String url = line.substring(commaPosition + 1);
                    if (!(client.length() == 0) && !(url.length() == 0)) {
                        Log.d(TAG, "Config Override: client=" + client + " , url=" + url);
                        API_URL = url;
                        API_CLIENT = client;
                        Toast.makeText(this, "API Client Override: " + API_CLIENT,
                                Toast.LENGTH_LONG)
                                .show();
                        return true;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return false;
    }

    private boolean haveValidData() {
        // Need valid preference data and more destinations
        return mPreferences.hasValidData() &&
                mDbHelper.getLastDeparture() > SantaPreferences.getCurrentTime();
    }

    /**
     * Access the INFO API, returns the delay in ms when the API should be accessed again
     */
    private long accessInfoAPI() {
        // Access the Info API
        mState = SantaServiceMessages.STATUS_PROCESSING;

        // Construct URL
        final String url = String.format(Locale.US, API_URL, API_CLIENT,
                mPreferences.getRandValue(), mPreferences.getRouteOffset(), mPreferences.getStreamOffset(),  TIMEZONE, LANGUAGE,
                mPreferences.getFingerprint());

        Log.d(TAG, "Tracking Santa.");
        long result = mApiProcessor.accessAPI(url);
        if (result < 0) {
            // API access was unsuccessful, back-off and try again later
            // Calculate delay, up to the max backoff time
            long delay = (long) Math.min((mBackoff * BACKOFF_FACTOR), MAX_BACKOFF_TIME);
            Log.d(TAG, "Couldn't communicate with Santa, trying again in: " + delay);
            mBackoff = delay;

            // Notify clients that there was an error and set state
            if (haveValidData()) {
                mState = SantaServiceMessages.STATUS_ERROR;
                sendMessage(Message.obtain(null, SantaServiceMessages.MSG_ERROR));
            } else {
                mState = SantaServiceMessages.STATUS_ERROR_NODATA;
                sendMessage(Message.obtain(null, SantaServiceMessages.MSG_ERROR_NODATA));
            }
            return delay;

        } else {
            SantaLog.d(TAG, "Accessed API, next access in: " + result);
            // reset back-off time
            mBackoff = INITIAL_BACKOFF_TIME;

            // Notify clients that API access was successful
            sendMessage(Message.obtain(null, SantaServiceMessages.MSG_SUCCESS));
            mState = SantaServiceMessages.STATUS_IDLE;

            return result;
        }

    }

    @Override
    public void onNewSwitchOffState(boolean isOff) {
        sendMessage(SantaServiceMessages.getSwitchOffMessage(isOff));
    }

    @Override
    public void onNewFingerprint() {
        sendMessage(Message.obtain(null, SantaServiceMessages.MSG_UPDATED_FINGERPRINT));
    }

    @Override
    public void onNewOffset() {
        sendMessage(getTimeUpdateMessage());
    }

    @Override
    public void onNewRouteLoaded() {
        sendMessage(Message.obtain(null, SantaServiceMessages.MSG_UPDATED_ROUTE));

        // Send a time update message, to ensure we don't leave the client in a state where it
        // thinks it has a route but no timestamp information.
        onNewOffset();
    }

    @Override
    public void onNewStreamLoaded() {
        sendMessage(Message.obtain(null, SantaServiceMessages.MSG_UPDATED_STREAM));
    }

    @Override
    public void onNewNotificationStreamLoaded() {
        sendMessage(Message.obtain(null, SantaServiceMessages.MSG_UPDATED_WEARSTREAM));
    }

    @Override
    public void notifyRouteUpdating() {
        sendMessage(Message.obtain(null, SantaServiceMessages.MSG_INPROGRESS_UPDATE_ROUTE));
    }

    @Override
    public void onNewCastState(boolean isDisabled) {
        sendMessage(SantaServiceMessages.getCastDisabledMessage(isDisabled));
    }

    @Override
    public void onNewGameState(GameDisabledState state) {
        sendMessage(SantaServiceMessages.getGamesMessage(state));
    }

    @Override
    public void onNewVideos(String video1, String video15, String video23) {
        sendMessage(SantaServiceMessages.getVideosMessage(video1, video15, video23));
    }

    @Override
    public void onNewDestinationPhotoState(boolean isDisabled) {
        sendMessage(SantaServiceMessages.getDestinationPhotoMessage(isDisabled));
    }

    @Override
    public void onNewApiDataAvailable() {
        // Force an API update immediately.
        mPreferences.setNextInfoAPIAccess(-1);
        scheduleApiAccess();
    }

    private void sendMessage(Message msg) {
        for (int i = 0; i < mClients.size(); i++) {
            try {
                // TODO - Message below is duplicated to avoid
                // IllegalStateException regarding queued messages.
                // Is there a cleaner way to do this or avoid altogether?
                Message target = new Message();
                target.copyFrom(msg);
                mClients.get(i).send(target);
            } catch (RemoteException e) {
                // Could not communicate with client, remove
                mClients.remove(i);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startHandlerThread();
        scheduleApiAccess();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        startHandlerThread();
        scheduleApiAccess();
        return mIncomingMessenger.getBinder();

    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mPendingClients.isEmpty() && mClients.isEmpty()) {
            // No clients connected, remove scheduled API execution
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                SantaLog.d(TAG, "last client unbind, removed scheduled threads");
            }
        }
        return super.onUnbind(intent);
    }

    private void startHandlerThread() {
        if (mHandler == null || !mApiThread.isAlive()) {
            SantaLog.d(TAG, "startHandlerThread");
            mApiThread.start();
            mHandler = new Handler(mApiThread.getLooper());
        }
    }

    private void startUpdateConfig() {
        // Using a ThreadPoolExecutor here because I could not figure out how to get the
        // Handler to execute this runnable.  Calling mHandler.post(...) never called run().
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                accessInfoAPI();
            }
        });
    }

    private Message getTimeUpdateMessage() {
        final long offset = mPreferences.getOffset();
        final long firstDeparture = mDbHelper.getFirstDeparture();
        final long finalArrival = mDbHelper.getLastArrival();
        final long finalDeparture = mDbHelper.getLastDeparture();
        return SantaServiceMessages.getTimeUpdateMessage(
                offset, firstDeparture, finalArrival, finalDeparture);
    }

    /**
     * Send the current state of the application to all pending clients.
     */
    private synchronized void sendPendingState() {

        if (!mPendingClients.isEmpty()) {
            final Message[] messages = new Message[]{
                    SantaServiceMessages.getBeginFullStateMessage(),
                    SantaServiceMessages.getSwitchOffMessage(mPreferences.getSwitchOff()),
                    getTimeUpdateMessage(),
                    SantaServiceMessages.getCastDisabledMessage(mPreferences.getCastDisabled()),
                    SantaServiceMessages.getGamesMessage(new GameDisabledState(mPreferences)),
                    SantaServiceMessages
                            .getDestinationPhotoMessage(mPreferences.getDestinationPhotoDisabled()),
                    SantaServiceMessages.getStateMessage(mState),
                    SantaServiceMessages.getVideosMessage(mPreferences.getVideos())
            };

            for (int i = 0; i < mPendingClients.size(); i++) {
                final Messenger messenger = mPendingClients.get(i);

                try {
                    for (Message msg : messages) {
                        messenger.send(msg);
                    }
                    // mark client as active
                    mClients.add(messenger);
                } catch (RemoteException e) {
                    // client is dead, ignore client
                }
                mPendingClients.remove(i);
            }
        }
    }

    /**
     * Handler for communication from a client to this Service. Registers and unregisters clients.
     */
    static class IncomingHandler extends Handler {

        private final WeakReference<SantaService> mServiceRef;

        IncomingHandler(SantaService service) {
            mServiceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            SantaService service = mServiceRef.get();
            if (service == null) {
                return;
            }
            switch (msg.what) {
                case SantaServiceMessages.MSG_SERVICE_REGISTER_CLIENT:
                    Messenger m = msg.replyTo;
                    service.mPendingClients.add(m);

                    if (service.mState != SantaServiceMessages.STATUS_PROCESSING) {
                        // send data if the background process is not currently running
                        synchronized (service.mPendingClients) {
                            service.sendPendingState();
                        }
                        service.scheduleApiAccess();
                    } else {
                        // Other state, notify client right away
                        try {
                            m.send(SantaServiceMessages.getStateMessage(service.mState));
                        } catch (RemoteException e) {
                            // Could not contact client, remove from pending list
                            service.mPendingClients.remove(m);
                        }
                    }
                    break;
                case SantaServiceMessages.MSG_SERVICE_UNREGISTER_CLIENT:
                    // Attempt to remove client from active list, alternatively from pending list
                    if (!service.mClients.remove(msg.replyTo)) {
                        service.mPendingClients.remove(msg.replyTo);
                    }
                    break;
                case SantaServiceMessages.MSG_SERVICE_FORCE_SYNC:
                    // Attempt to sync right now (for debugging purposes)
                    Toast.makeText(service, "Starting sync.", Toast.LENGTH_SHORT).show();
                    service.startUpdateConfig();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    }

}
