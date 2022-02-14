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
package com.google.android.apps.santatracker.doodles.penguinswim;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.google.android.apps.santatracker.doodles.shared.ExternalStoragePermissions;
import com.google.android.apps.santatracker.doodles.shared.actor.Actor;
import com.google.android.apps.santatracker.util.SantaLog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class which handles the saving and loading of levels for the doodle games.
 *
 * <p>The {@code LevelManager} stores levels in a simple JSON format. The {@code Actor} classes
 * wishing to be managed by a {@code LevelManager} should handle their own serialization and
 * deserialization.
 */
public abstract class LevelManager<T extends TiltModel> {
    public static final String TAG = LevelManager.class.getSimpleName();
    private static final String ACTORS_KEY = "actors";

    protected final Context context;
    private final ExternalStoragePermissions storagePermissions;

    public LevelManager(Context context) {
        this(context, new ExternalStoragePermissions());
    }

    @VisibleForTesting
    LevelManager(Context context, ExternalStoragePermissions storagePermissions) {
        this.context = context;
        this.storagePermissions = storagePermissions;
    }

    /**
     * Saves a level to persistent storage.
     *
     * @param level The level to save.
     * @param filename The name of the level's file on disk. This file will be stored inside of a
     *     preset directory, defined by the LevelManager implementation.
     */
    public void saveLevel(T level, String filename) {
        File levelsDir = getLevelsDir();
        if (!levelsDir.exists() && !levelsDir.mkdirs()) {
            // If we are unable to find or make the desired output directory, log a warning and
            // fail.
            SantaLog.w(TAG, "Unable to reach dir: " + levelsDir.getAbsolutePath());
            return;
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(levelsDir, filename));
            saveLevel(level, outputStream);
        } catch (FileNotFoundException e) {
            SantaLog.w(TAG, "Unable to save file: " + filename);
        }
    }

    /**
     * Writes a level to the provided OutputStream.
     *
     * @param level The level to save.
     * @param outputStream The stream to which the level should be written.
     */
    @VisibleForTesting
    void saveLevel(T level, OutputStream outputStream) {
        try {
            saveActors(level.getActors(), outputStream);
        } catch (JSONException e) {
            SantaLog.w(TAG, "Unable to create level JSON.");
        } catch (IOException e) {
            SantaLog.w(TAG, "Unable to write actors to output stream.");
        }
    }

    /**
     * Loads a level from persistent storage.
     *
     * <p>
     *
     * <p>This loads first tries to load a level from assets, falling back to external storage if
     * necessary. If it still cannot load the level, the default level will be loaded.
     *
     * @param filename The name of the file to load. This file should be stored in a preset
     *     directory, specified by the LevelManager implementation.
     * @return The loaded level.
     */
    public T loadLevel(String filename) {
        if (filename == null) {
            SantaLog.w(TAG, "Couldn't load level with null filename, using default level instead.");
            return loadDefaultLevel();
        }

        BufferedReader externalStorageReader = null;
        try {
            externalStorageReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(new File(getLevelsDir(), filename)),
                                    "UTF-8"));
        } catch (IOException e) {
            SantaLog.d(TAG, "Unable to load file from external storage: " + filename);
        }

        BufferedReader assetsReader = null;
        try {
            assetsReader =
                    new BufferedReader(
                            new InputStreamReader(context.getAssets().open(filename), "UTF-8"));
        } catch (IOException e) {
            SantaLog.d(TAG, "Unable to load file from assets: " + filename);
        }

        T model = loadLevel(externalStorageReader, assetsReader);
        model.setLevelName(filename);
        return model;
    }

    /**
     * Loads a level from either assets, or from external storage.
     *
     * <p>
     *
     * <p>This first tries to load a level from assets, falling back to external storage if
     * necessary. If it still cannot load the level, the default level will be loaded.
     *
     * <p>
     *
     * <p>This method should only be used for testing LevelManager. Real use cases should generally
     * use {@code loadLevel(String filename)}.
     *
     * @param externalStorageReader
     * @param assetsInputReader
     * @return The loaded level.
     */
    @VisibleForTesting
    T loadLevel(BufferedReader externalStorageReader, BufferedReader assetsInputReader) {
        JSONObject json = null;
        if (assetsInputReader != null) {
            json = readLevelJson(assetsInputReader);
            SantaLog.d(TAG, "Loaded level from assets.");
        }
        if (json == null
                && externalStorageReader != null
                && storagePermissions.isExternalStorageReadable()) {
            json = readLevelJson(externalStorageReader);
            SantaLog.d(TAG, "Loaded level from external storage.");
        }
        if (json == null) {
            SantaLog.w(TAG, "Couldn't load level data, using default level instead.");
            return loadDefaultLevel();
        }

        T model = getEmptyModel();
        try {
            JSONArray actors = json.getJSONArray(ACTORS_KEY);
            for (int i = 0; i < actors.length(); i++) {
                Actor actor = loadActorFromJSON(actors.getJSONObject(i));
                if (actor != null) {
                    model.addActor(actor);
                }
            }
        } catch (JSONException e) {
            SantaLog.w(TAG, "Couldn't load actors, using default level instead.");
            return loadDefaultLevel();
        }
        return model;
    }

    /**
     * Initializes the default level and returns it.
     *
     * <p>
     *
     * <p>In general, this should be an empty or minimal level.
     *
     * @return the initialized default level.
     */
    public abstract T loadDefaultLevel();

    /**
     * Returns the external storage directory within which levels of this type should be saved.
     *
     * @return The base directory which should be used to save levels.
     */
    protected abstract File getLevelsDir();

    /**
     * Loads a single actor from JSON.
     *
     * @param json The JSON representation of the actor to be loaded
     * @return The loaded actor, or null if the actor could not be loaded.
     * @throws JSONException if the JSON is malformed, or fails to be parsed.
     */
    @VisibleForTesting
    abstract Actor loadActorFromJSON(JSONObject json) throws JSONException;

    /**
     * Returns an empty, or minimal model of the appropriate type. Generally, this should be the
     * same as asking for a {@code new T()}.
     *
     * @return The empty model.
     */
    protected abstract T getEmptyModel();

    /**
     * Returns a JSONArray containing the JSON representation of the passed-in list of actors.
     *
     * <p>
     *
     * <p>If a given actor does not provide a JSON representation, it will not appear in the
     * returned JSONArray.
     *
     * @param actors The actors to convert into JSON.
     * @return The JSON representation of the list of actors.
     * @throws JSONException If the parsing of an actor into JSON fails.
     */
    @VisibleForTesting
    JSONArray getActorsJson(List<Actor> actors) throws JSONException {
        JSONArray actorsJson = new JSONArray();
        for (Actor actor : actors) {
            JSONObject json = actor.toJSON();
            if (json != null) {
                actorsJson.put(json);
            }
        }
        return actorsJson;
    }

    /**
     * Writes a list of actors to an OutputStream.
     *
     * @param actors The actors to be written.
     * @param outputStream The OuputStream which should be used to write the list of actors.
     * @throws JSONException If the conversion of actors into JSON fails.
     * @throws IOException If we fail to write to the OutputStream.
     */
    private void saveActors(List<Actor> actors, OutputStream outputStream)
            throws JSONException, IOException {
        JSONObject json = new JSONObject();
        json.put(ACTORS_KEY, getActorsJson(actors));
        SantaLog.d(TAG, json.toString(2));

        if (storagePermissions.isExternalStorageWritable()) {
            writeLevelJson(json, outputStream);
        } else {
            SantaLog.w(TAG, "External storage is not writable");
        }
    }

    /**
     * Writes a JSONObject to an OutputStream.
     *
     * @param json The object to be written.
     * @param outputStream The stream to be written to.
     * @throws IOException If we fail to write to the OutputStream.
     */
    private void writeLevelJson(JSONObject json, OutputStream outputStream) throws IOException {
        outputStream.write(json.toString().getBytes());
        outputStream.close();
    }

    /**
     * Read a JSONObject from a BufferedReader.
     *
     * @param reader The BufferedReader which contains the JSON to be read.
     * @return A JSONObject parsed from the contents of the BufferedReader.
     */
    private JSONObject readLevelJson(BufferedReader reader) {
        try {
            String levelData = "";
            String line = reader.readLine();
            while (line != null) {
                levelData += line;
                line = reader.readLine();
            }
            reader.close();
            return new JSONObject(levelData);
        } catch (IOException e) {
            SantaLog.w(TAG, "readLevelJson: Couldn't read JSON.");
        } catch (JSONException e) {
            SantaLog.w(TAG, "readLevelJson: Couldn't create JSON.");
        }
        return null;
    }
}
