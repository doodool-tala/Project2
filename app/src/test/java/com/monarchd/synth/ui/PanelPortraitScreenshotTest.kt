package com.monarchd.synth.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.monarchd.synth.SynthViewModel
import com.monarchd.synth.ui.theme.MonarchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h780dp-port-xhdpi")
class PanelPortraitScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renderPortraitPanel() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("monarch_d", Context.MODE_PRIVATE).edit().clear().commit()
        val viewModel = SynthViewModel(app)
        viewModel.dismissIntro()

        compose.setContent {
            MonarchTheme {
                SynthApp(viewModel = viewModel, enableAudio = false)
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(120)
        capture("06-panel-portrait-osc")
    }

    @Test
    fun renderPortraitIntro() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("monarch_d", Context.MODE_PRIVATE).edit().clear().commit()
        val viewModel = SynthViewModel(app)

        compose.setContent {
            MonarchTheme {
                SynthApp(viewModel = viewModel, enableAudio = false)
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(120)
        capture("07-panel-portrait-intro")
    }

    private fun capture(name: String) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metrics = app.resources.displayMetrics
        val decor = compose.activity.window.decorView
        val width = if (decor.width > 0) decor.width else metrics.widthPixels
        val height = if (decor.height > 0) decor.height else metrics.heightPixels
        if (decor.width == 0 || decor.height == 0) {
            decor.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, width, height)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        val directory = File("build/screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
