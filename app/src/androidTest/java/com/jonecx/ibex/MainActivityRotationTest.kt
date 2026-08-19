package com.jonecx.ibex

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// Rotation must be a configuration change, never a recreation: recreation restarts video playback.
class MainActivityRotationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun restoreOrientation() {
        composeTestRule.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @Test
    fun rotatingTheDeviceDoesNotRecreateTheActivity() {
        var recreations = 0
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is MainActivity) recreations++
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        }
        val application = composeTestRule.activity.application
        application.registerActivityLifecycleCallbacks(callbacks)
        try {
            val startOrientation = composeTestRule.activity.resources.configuration.orientation
            val target = if (startOrientation == Configuration.ORIENTATION_PORTRAIT) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            composeTestRule.activityRule.scenario.onActivity { it.requestedOrientation = target }
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.activity.resources.configuration.orientation != startOrientation
            }
            composeTestRule.waitForIdle()
            assertEquals(0, recreations)
        } finally {
            application.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }
}
