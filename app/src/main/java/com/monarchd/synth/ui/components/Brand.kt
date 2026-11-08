package com.monarchd.synth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.monarchd.synth.ui.theme.Amber

/** The Monarch "M" waveform inside a thin brass ring. */
@Composable
fun LogoMark(modifier: Modifier = Modifier, color: Color = Amber) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val radius = minOf(w, h) / 2f - 1.dp.toPx()
        drawCircle(
            color = Color(0xFF8E6A34),
            radius = radius,
            style = Stroke(1.dp.toPx())
        )
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.72f)
            lineTo(w * 0.28f, h * 0.34f)
            lineTo(w * 0.41f, h * 0.58f)
            lineTo(w * 0.50f, h * 0.29f)
            lineTo(w * 0.59f, h * 0.58f)
            lineTo(w * 0.72f, h * 0.34f)
            lineTo(w * 0.72f, h * 0.72f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = (w * 0.075f).coerceAtLeast(1.2.dp.toPx()),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawRect(
            color = Color.Transparent,
            size = Size(w, h)
        )
    }
}
