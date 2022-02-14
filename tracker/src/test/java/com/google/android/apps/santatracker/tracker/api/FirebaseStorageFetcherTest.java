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

package com.google.android.apps.santatracker.tracker.api;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.android.apps.santatracker.tracker.BuildConfig;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/** Test for {@link FirebaseStorageFetcher}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26, constants = BuildConfig.class)
public class FirebaseStorageFetcherTest {

    private static final String FILE_NAME = "routes/test-file.json";
    private static final String FILE_CONTENTS = "HELLO WORLD";
    private static long TASK_TIMEOUT_MS = 30 * 1000;

    private FirebaseStorage mStorage;
    private SharedPreferences mPreferences;
    private FirebaseStorageFetcher mFetcher;

    @Before
    public void setUp() {
        // Context and logging
        Context context = RuntimeEnvironment.application;
        ShadowLog.stream = System.out;

        // Initialize dummy firebase app
        try {
            FirebaseApp.initializeApp(
                    context,
                    new FirebaseOptions.Builder()
                            .setApiKey("FAKE_API_KEY")
                            .setApplicationId("FAKE_APPLICATION_ID")
                            .build());
        } catch (IllegalStateException e) {
            // No-op, happens when app is double-initialized
        }

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        mStorage = mock(FirebaseStorage.class);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mFetcher = new FirebaseStorageFetcher(context, mPreferences, mStorage, executor);
    }

    @After
    public void tearDown() {
        // Clear all shared prefs
        mPreferences.edit().clear().apply();
    }

    @Test
    public void testFreshDownload() throws Exception {
        StorageReference reference = getMockStorageReference(FILE_NAME);

        // Mock updated time to return the current time (so cache is never hit)
        long currentTime = System.currentTimeMillis();
        stubFirebaseStorageUpdatedTime(reference, currentTime);

        // Mock file contents
        stubFirebaseStorageContents(reference, FILE_CONTENTS);

        // Load the test file
        Task<String> loadTask = waitForTask(mFetcher.get(FILE_NAME, 0, TimeUnit.SECONDS));

        // Sanity check for proper results
        assertEquals(loadTask.getResult(), FILE_CONTENTS);
    }

    /** Wait for a task to be complete, avoids Tasks.await() main thread complaints. */
    private <T> Task<T> waitForTask(final Task<T> task) throws Exception {
        final long startTime = System.currentTimeMillis();
        Thread thread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                while (!task.isComplete()) {
                                    if (System.currentTimeMillis() - startTime > TASK_TIMEOUT_MS) {
                                        throw new RuntimeException("Timed out waiting for task.");
                                    }

                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        fail("Error waiting for task: " + e.getLocalizedMessage());
                                    }
                                }
                            }
                        });

        thread.run();
        thread.join();

        return task;
    }

    /** Get a mocked storage reference for a file path. */
    private StorageReference getMockStorageReference(String path) {
        StorageReference reference = mock(StorageReference.class);
        when(mStorage.getReference(path)).thenReturn(reference);

        return reference;
    }

    /** Stub the last updated time of a file in Firebase Storage */
    public void stubFirebaseStorageUpdatedTime(StorageReference reference, long time) {
        StorageMetadata metadata = mock(StorageMetadata.class);
        when(metadata.getUpdatedTimeMillis()).thenReturn(time);

        when(reference.getMetadata()).thenReturn(Tasks.forResult(metadata));
    }

    /** Stub the contents of a file in Firebase Storage. */
    private void stubFirebaseStorageContents(StorageReference reference, String contents) {
        Task<byte[]> completed = (Task<byte[]>) mock(Task.class);
        when(completed.isComplete()).thenReturn(true);
        when(completed.isSuccessful()).thenReturn(true);
        when(completed.getResult()).thenReturn(contents.getBytes());

        when(reference.getBytes(anyLong())).thenReturn(completed);
    }

    /** Populate the cache with file contents. */
    private void stubCacheContents(String path, String contents) throws IOException {
        when(mFetcher.cachedCopyExists(path)).thenReturn(true);
        when(mFetcher.getCachedCopy(path)).thenReturn(contents);
    }

    /** Make getBytes() calls on a file immediate invoke failure listeners. */
    private void stubFirebaseStorageError(StorageReference reference, Exception exception) {
        final Task<byte[]> task = (Task<byte[]>) mock(Task.class);
        when(task.isComplete()).thenReturn(true);
        when(task.isSuccessful()).thenReturn(false);
        when(task.getException()).thenReturn(exception);

        when(task.addOnFailureListener(any(OnFailureListener.class)))
                .thenAnswer(
                        new Answer<Task<byte[]>>() {
                            @Override
                            public Task<byte[]> answer(InvocationOnMock invocation)
                                    throws Throwable {
                                OnFailureListener listener =
                                        (OnFailureListener) invocation.getArguments()[0];
                                listener.onFailure(task.getException());

                                return task;
                            }
                        });

        when(reference.getBytes(anyLong())).thenReturn(task);
    }
}
