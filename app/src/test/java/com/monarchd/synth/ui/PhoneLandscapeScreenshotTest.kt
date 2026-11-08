package com.monarchd.synth.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class PhoneLandscapeScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun panelPages() {
        compose.setInstrument(newInstrumentViewModel())
        captureScreenshot(compose.activity, "phone-landscape-panel-01-osc")

        val pager = compose.onNodeWithTag("panelPager")
        listOf("mix", "filter", "envelopes", "performance").forEachIndexed { index, page ->
            pager.performTouchInput { swipeLeft() }
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(500)
            captureScreenshot(compose.activity, "phone-landscape-panel-0${index + 2}-$page")
        }
    }

    @Test
    fun settingsAndAbout() {
        compose.setInstrument(newInstrumentViewModel())
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.settleSheets()
        captureScreenshot(compose.activity, "phone-landscape-settings")

        compose.onNodeWithText("© 2026 Monarch Instruments").performScrollTo()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(200)
        captureScreenshot(compose.activity, "phone-landscape-settings-about")
    }

    @Test
    fun help() {
        compose.setInstrument(newInstrumentViewModel())
        compose.onNodeWithContentDescription("How to play").performClick()
        compose.settleSheets()
        captureScreenshot(compose.activity, "phone-landscape-help")
    }

    @Test
    fun patchLibrary() {
        val viewModel = newInstrumentViewModel()
        compose.setInstrument(viewModel)
        compose.onNodeWithContentDescription("Preset ${viewModel.patch.name}. Open the patch library.")
            .performClick()
        compose.settleSheets()
        captureScreenshot(compose.activity, "phone-landscape-library")
    }

    @Test
    fun intro() {
        compose.setInstrument(newInstrumentViewModel(dismissIntro = false))
        captureScreenshot(compose.activity, "phone-landscape-intro")
    }
}
