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
package com.google.android.apps.santatracker.doodles.shared;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Maintains the history and stats of what the user has accomplished.
 *
 * <p>Note that this class handles the serializing into JSON instead of each game.  This was done
 * to make it easier to make a game picker that showed your status on each game.  Since there are
 * canonical types it would then know how to read them.  We add a setArbitraryData and
 * getArbitaryData for any game that wants to put other kind of information in.
 */
public class HistoryManager {
  private static final String TAG = HistoryManager.class.getSimpleName();

  public static final String BEST_PLACE_KEY = "place";
  public static final String BEST_STAR_COUNT_KEY = "stars";
  public static final String BEST_TIME_MILLISECONDS_KEY = "time";
  public static final String BEST_SCORE_KEY = "score";
  public static final String BEST_DISTANCE_METERS_KEY = "distance";
  public static final String ARBITRARY_DATA_KEY = "arb";

  private static final String FILENAME = "history.json";

  private final Context context;
  private volatile JSONObject history;

  /**
   * Listener for when the history is loaded.
   */
  public static interface HistoryListener {
    public void onFinishedLoading();
    public void onFinishedSaving();
  }
  private HistoryListener listener;

  /**
   * Creates a history manager.
   * HistoryListener can be null.
   */
  public HistoryManager(Context context, HistoryListener listener) {
    this.context = context;
    this.listener = listener;
    // While history is loading from disk, we ignore any changes clients might ask for.
    history = null;
    load();
  }

  public void setListener(HistoryListener listener) {
    this.listener = listener;
  }

  /**
   * Gets the json object for a particular game type.
   */
  private JSONObject getGameObject(GameType gameType) throws JSONException {
    if (history == null) {
      throw new JSONException("null history");
    }
    JSONObject gameObject = history.optJSONObject(gameType.toString());
    if (gameObject == null) {
      gameObject = new JSONObject();
    }
    return gameObject;
  }

  /************************ Setters *****************************/
  /**
   * Set the best place (1st, 2nd, 3rd) for a game type.
   * NOTE: It's expected for the client to figure out if it is the best place.
   */
  public void setBestPlace(GameType gameType, int place) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      gameObject.put(BEST_PLACE_KEY, place);
      history.put(gameType.toString(), gameObject);
    } catch (JSONException e) {
      Log.e(TAG, "error setting place", e);
    }
  }

  /**
   * Set the best star count for a game type.
   * NOTE: It's expected for the client to figure out if it is the best star count.
   */
  public void setBestStarCount(GameType gameType, int count) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      gameObject.put(BEST_STAR_COUNT_KEY, count);
      history.put(gameType.toString(), gameObject);
    } catch (JSONException e) {
      Log.e(TAG, "error setting place", e);
    }
  }

  /**
   * Set the best time for a game type.
   * NOTE: it's expected for the client to figure out if it is the best time since some will want
   * bigger and some will want smaller numbers.
   */
  public void setBestTime(GameType gameType, long timeInMilliseconds) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      gameObject.put(BEST_TIME_MILLISECONDS_KEY, timeInMilliseconds);
      history.put(gameType.toString(), gameObject);
    } catch (JSONException e) {
      Log.e(TAG, "error setting time", e);
    }
  }

  /**
   * Set the best score for a game type.
   * NOTE: it's expected for the client to figure out if it is the best score since some will want
   * bigger and some will want smaller numbers.
   */
  public void setBestScore(GameType gameType, double score) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      gameObject.put(BEST_SCORE_KEY, score);
      history.put(gameType.toString(), gameObject);
    } catch (JSONException e) {
      Log.e(TAG, "error setting score", e);
    }
  }

  /**
   * Set the best distance for a game type.
   * NOTE: it's expected for the client to figure out if it is the best distance since some will
   * want bigger and some will want smaller numbers.
   */
  public void setBestDistance(GameType gameType, double distanceInMeters) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      gameObject.put(BEST_DISTANCE_METERS_KEY, distanceInMeters);
      history.put(gameType.toString(), gameObject);
    } catch (JSONException e) {
      Log.e(TAG, "error setting distance", e);
    }
  }

  /**
   * Sets an arbitrary jsonObject a game might want.
   */
  public void setArbitraryData(GameType gameType, JSONObject data) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      gameObject.put(ARBITRARY_DATA_KEY, data);
      history.put(gameType.toString(), gameObject);
    } catch (JSONException e) {
      Log.e(TAG, "error setting distance", e);
    }
  }

  /************************ Getters *****************************/

  /**
   * Returns the best place so far.  Null if no value has been given yet.
   */
  public Integer getBestPlace(GameType gameType) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      return gameObject.getInt(BEST_PLACE_KEY);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Returns the best star count so far.  Null if no value has been given yet.
   */
  public Integer getBestStarCount(GameType gameType) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      return gameObject.getInt(BEST_STAR_COUNT_KEY);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Returns the best time so far.  Null if no value has been given yet.
   */
  public Long getBestTime(GameType gameType) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      return gameObject.getLong(BEST_TIME_MILLISECONDS_KEY);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Returns the best score so far.  Null if no value has been given yet.
   */
  public Double getBestScore(GameType gameType) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      return gameObject.getDouble(BEST_SCORE_KEY);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Returns the best distance so far.  Null if no value has been given yet.
   */
  public Double getBestDistance(GameType gameType) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      return gameObject.getDouble(BEST_DISTANCE_METERS_KEY);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Returns arbitrary JSONObject a game might want.  Null if no value has been given yet.
   */
  public JSONObject getArbitraryData(GameType gameType) {
    try {
      JSONObject gameObject = getGameObject(gameType);
      return gameObject.getJSONObject(ARBITRARY_DATA_KEY);
    } catch (JSONException e) {
      return null;
    }
  }

  /********************** File Management **************************/
  /**
   * Saves the file in the background.
   */
  public void save() {
    new AsyncTask<Void, Void, Void> () {
      @Override
      protected Void doInBackground(Void... params) {
        try {
          FileOutputStream outputStream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
          byte[] bytes = history.toString().getBytes();
          outputStream.write(bytes);
          outputStream.close();
          Log.i(TAG, "Saved: " + history);
        } catch (IOException e) {
          Log.w(TAG, "Couldn't save JSON at: " + FILENAME);
        } catch (Exception e) {
          Log.w(TAG, "Crazy exception happened", e);
        }
        return null;
      }
      @Override
      protected void onPostExecute(Void result) {
        if (listener != null) {
          listener.onFinishedSaving();
        }
      }
    }.execute();
  }

  /**
   * Loads the history object from file.  Then merges with any changes that might have occured while
   * we waited for it to load.
   */
  private void load() {
    new AsyncTask<Void, Void, Void> () {
      @Override
      protected Void doInBackground(Void... params) {
        try {
          File file = new File(context.getFilesDir(), FILENAME);
          int length = (int) file.length();
          if (length <= 0) {
            history = new JSONObject();
            return null;
          }

          byte[] bytes = new byte[length];
          FileInputStream inputStream = new FileInputStream(file);
          inputStream.read(bytes);
          inputStream.close();

          history = new JSONObject(new String(bytes, "UTF-8"));
          Log.i(TAG, "Loaded: " + history);
        } catch (JSONException e) {
          Log.w(TAG, "Couldn't create JSON for: " + FILENAME);
        } catch (UnsupportedEncodingException e) {
          Log.d(TAG, "Couldn't decode: " + FILENAME);
        } catch (IOException e) {
          Log.w(TAG, "Couldn't read history: " + FILENAME);
        }
        return null;
      }
      @Override
      protected void onPostExecute(Void result) {
        if (listener != null) {
          listener.onFinishedLoading();
        }
      }
    }.execute();
  }
}
