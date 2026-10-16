package com.mini.recorder.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's baseline profile covering the cold-start path.
 *
 * Requires a connected device/emulator:
 * `./gradlew :app:generateBaselineProfile`.
 * The generated profile is written to `app/src/main/baseline-prof.txt`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = "com.mini.recorder") {
            startActivityAndWait()
            device.waitForIdle()
        }
    }
}
