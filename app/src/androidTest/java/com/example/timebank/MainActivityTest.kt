package com.example.timebank

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setup() {
        // Clear shared preferences to ensure clean state before each test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("TimeBankPrefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testInitialState() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Check initial timer text
            onView(withId(R.id.timer_text)).check(matches(withText("00:00:00")))
            
            // Check start button
            onView(withId(R.id.start_button)).check(matches(withText("Start")))
            
            // Check default preset buttons
            onView(withId(R.id.add_default_preset_0_button)).check(matches(withText("Add 1 min")))
            onView(withId(R.id.add_default_preset_1_button)).check(matches(withText("Add 5 min")))
            onView(withId(R.id.add_default_preset_2_button)).check(matches(withText("Add 10 min")))
        }
    }

    @Test
    fun testAddOneMinute() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Click Add 1 min
            onView(withId(R.id.add_default_preset_0_button)).perform(click())
            
            // Check timer text updates to 1 minute
            onView(withId(R.id.timer_text)).check(matches(withText("00:01:00")))
        }
    }

    @Test
    fun testAddMultipleTimes() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Add 1 min
            onView(withId(R.id.add_default_preset_0_button)).perform(click())
            // Add 5 sec
            onView(withId(R.id.add_5_sec_button)).perform(click())
            
            // Check timer text updates to 1 minute 5 seconds
            onView(withId(R.id.timer_text)).check(matches(withText("00:01:05")))
        }
    }
    
    @Test
    fun testReset() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Add 1 min
            onView(withId(R.id.add_default_preset_0_button)).perform(click())
            onView(withId(R.id.timer_text)).check(matches(withText("00:01:00")))
            
            // Click Reset
            onView(withId(R.id.reset_button)).perform(click())
            
            // Check timer text goes back to 0
            onView(withId(R.id.timer_text)).check(matches(withText("00:00:00")))
        }
    }
}
