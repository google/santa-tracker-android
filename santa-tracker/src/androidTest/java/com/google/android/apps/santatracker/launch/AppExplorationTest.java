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
package com.google.android.apps.santatracker.launch;

import static androidx.test.InstrumentationRegistry.getContext;
import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.InstrumentationRegistry.getTargetContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.Matchers.allOf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A set of tests to explore the App UI. These are meant to test for fatal errors on a variety of
 * devices by exploring the application. They do not verify that individual UI elements are
 * correctly displayed.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AppExplorationTest {

    @Rule
    public final ActivityTestRule<StartupActivity> mActivityTestRule =
            new ActivityTestRule<StartupActivity>(StartupActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    // Standard intent, but disable animations
                    Context targetContext =
                            InstrumentationRegistry.getInstrumentation().getTargetContext();
                    Intent result = new Intent(targetContext, StartupActivity.class);
                    result.putExtra(StartupActivity.EXTRA_DISABLE_ANIMATIONS, true);
                    return result;
                }
            };

    LaunchCollection mGameLaunchCollection;
    private SplashIdlingResource mSplashIdlingResource;
    private IdlingRegistry mIdlingRegistry;

    @BeforeClass
    public static void grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getTargetContext().getPackageName();
            String testPackageName = packageName + ".test";

            // Grant "WRITE_EXTERNAL_STORAGE"
            grantPermission(packageName, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            grantPermission(testPackageName, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            // Grant "ACCESS_FINE_LOCATION"
            grantPermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION);
            grantPermission(testPackageName, Manifest.permission.ACCESS_FINE_LOCATION);

            // Grant "GET_TASKS"
            grantPermission(packageName, Manifest.permission.GET_TASKS);
            grantPermission(testPackageName, Manifest.permission.GET_TASKS);
        }
    }

    @SuppressLint("NewApi")
    private static void grantPermission(String packageName, String perm) {
        getInstrumentation()
                .getUiAutomation()
                .executeShellCommand("pm grant " + packageName + " " + perm);
    }

    @Before
    public void registerIdlingResources() {
        mIdlingRegistry = IdlingRegistry.getInstance();
        mSplashIdlingResource = new SplashIdlingResource(getContext());
        mIdlingRegistry.register(mSplashIdlingResource);
        mGameLaunchCollection = mActivityTestRule.getActivity().getGameLaunchCollection();
    }

    @After
    public void unregisterIdlingResources() {
        mIdlingRegistry.unregister(mSplashIdlingResource);
    }

    /** Test navigating through the PresentQuest UI */
    @Test
    public void presentQuestTest() {
        LaunchCollection gameLaunchCollection =
                mActivityTestRule.getActivity().getGameLaunchCollection();

        // Open Present Quest
        ViewInteraction recyclerView =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.card_list),
                                isDisplayed()));
        int presentQuestPosition =
                gameLaunchCollection.getPositionFromCardKey(CardKeys.PRESENT_QUEST_CARD);
        recyclerView.perform(
                actionOnItemAtPosition(presentQuestPosition, scrollTo()),
                actionOnItemAtPosition(presentQuestPosition, click()));

        // Click on the "location" fab
        ViewInteraction floatingActionButton =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.fab_location),
                                isDisplayed()));
        floatingActionButton.perform(click());

        // CLick on the user image
        ViewInteraction imageView =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.map_user_image),
                                isDisplayed()));
        imageView.perform(click());

        // Click the right arrow a few times
        ViewInteraction appCompatImageView =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_right),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView.perform(scrollTo(), click());

        ViewInteraction appCompatImageView2 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_right),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView2.perform(scrollTo(), click());

        ViewInteraction appCompatImageView3 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_right),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView3.perform(scrollTo(), click());

        // Click the left arrow a few times
        ViewInteraction appCompatImageView4 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_left),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView4.perform(scrollTo(), click());

        ViewInteraction appCompatImageView5 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_left),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView5.perform(scrollTo(), click());

        ViewInteraction appCompatImageView6 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_left),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView6.perform(scrollTo(), click());

        ViewInteraction appCompatImageView7 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_left),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView7.perform(scrollTo(), click());

        // Right arrow again
        ViewInteraction appCompatImageView8 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.arrow_right),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView8.perform(scrollTo(), click());

        // Go back to the map screen
        pressBack();

        // Click the user image again
        ViewInteraction imageView2 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.map_user_image),
                                isDisplayed()));
        imageView2.perform(click());

        // Click the button to edit the first workshop
        ViewInteraction appCompatImageView9 =
                onView(
                        allOf(
                                withId(com.google.android.apps.santatracker.R.id.button_edit_1),
                                withParent(
                                        withId(
                                                com.google
                                                        .android
                                                        .apps
                                                        .santatracker
                                                        .R
                                                        .id
                                                        .activity_profile))));
        appCompatImageView9.perform(scrollTo(), click());

        // Cancel workshop move by pressing back
        pressBack();

        // Go back to map screen
        pressBack();
    }
}
