package com.monarchd.synth.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import com.monarchd.synth.SynthViewModel
import com.monarchd.synth.ui.theme.MonarchTheme
import org.robolectric.shadows.ShadowDialog
import java.io.File

internal fun newInstrumentViewModel(dismissIntro: Boolean = true): SynthViewModel {
    val app = ApplicationProvider.getApplicationContext<Application>()
    app.getSharedPreferences("monarch_d", Context.MODE_PRIVATE).edit().clear().commit()
    return SynthViewModel(app).also { viewModel ->
        if (dismissIntro) viewModel.dismissIntro()
    }
}

internal fun ComposeContentTestRule.setInstrument(viewModel: SynthViewModel) {
    setContent {
        MonarchTheme {
            SynthApp(viewModel = viewModel, enableAudio = false)
        }
    }
    waitForIdle()
    mainClock.advanceTimeBy(150)
}

internal fun ComposeContentTestRule.settleSheets() {
    waitForIdle()
    mainClock.advanceTimeBy(650)
    waitForIdle()
}

internal fun captureScreenshot(activity: ComponentActivity, name: String) {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val metrics = app.resources.displayMetrics
    val decor = activity.window.decorView
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
    val canvas = Canvas(bitmap)
    decor.draw(canvas)

    // Compose modal bottom sheets live in a separate dialog window. Capture
    // those real windows as well so the screenshot shows the sheet and its
    // scrim rather than only the activity underneath it.
    ShadowDialog.getShownDialogs().forEach { dialog ->
        val dialogDecor = dialog.window?.decorView ?: return@forEach
        if (dialogDecor.width <= 0 || dialogDecor.height <= 0) {
            dialogDecor.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            dialogDecor.layout(0, 0, width, height)
        }
        dialogDecor.draw(canvas)
    }

    val directory = File("build/screenshots").apply { mkdirs() }
    File(directory, "$name.png").outputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
}
