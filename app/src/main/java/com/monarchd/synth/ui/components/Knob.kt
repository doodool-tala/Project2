package com.monarchd.synth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monarchd.synth.ui.theme.Amber
import com.monarchd.synth.ui.theme.AmberBright
import com.monarchd.synth.ui.theme.Cream
import com.monarchd.synth.ui.theme.CreamDim
import com.monarchd.synth.ui.theme.CreamFaint
import com.monarchd.synth.ui.theme.PanelFont
import com.monarchd.synth.ui.theme.ReadoutFont
import com.monarchd.synth.ui.theme.Steel
import com.monarchd.synth.ui.theme.SteelDark
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A physical-feeling rotary control. Vertical drag changes the value; horizontal
 * movement offers fine adjustment. Double-tap restores the default. The active
 * arc and the tooltip follow the finger.
 */
@Composable
fun SynthKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 48.dp,
    defaultValue: Float = 0.5f,
    valueText: String = "${(value * 100).toInt()}",
    accent: Color = Amber,
    enabled: Boolean = true
) {
    val hapticsEnabled = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    val latestValue by rememberUpdatedState(value)
    val latestChange by rememberUpdatedState(onValueChange)
    val dragSpan = with(LocalDensity.current) { 155.dp.toPx() }

    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(value) }

    val dragModifier = Modifier.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectDragGestures(
            onDragStart = {
                dragValue = latestValue
                dragging = true
            },
            onDragEnd = { dragging = false },
            onDragCancel = { dragging = false }
        ) { change, dragAmount ->
            change.consume()
            val delta = -dragAmount.y / dragSpan + dragAmount.x / (dragSpan * 7f)
            dragValue = (dragValue + delta).coerceIn(0f, 1f)
            latestChange(dragValue)
        }
    }

    val tapModifier = Modifier.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectTapGestures(
            onDoubleTap = {
                latestChange(defaultValue)
                if (hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = if (dragging) Cream else CreamDim,
            fontFamily = PanelFont,
            fontWeight = FontWeight.Medium,
            fontSize = 7.5.sp,
            letterSpacing = 1.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(diameter + 10.dp)
                .then(dragModifier)
                .then(tapModifier)
                .semantics {
                    contentDescription = label
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
                    setProgress { target ->
                        latestChange(target.coerceIn(0f, 1f))
                        true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = min(size.width, size.height) / 2f - 1.dp.toPx()
                val bodyRadius = outerRadius - 5.dp.toPx()
                val startAngle = 135f
                val sweep = 270f

                // Detent ticks around the dial.
                var i = 0
                while (i <= 10) {
                    val angle = Math.toRadians((startAngle + sweep * i / 10f).toDouble()).toFloat()
                    val direction = Offset(cos(angle), sin(angle))
                    val active = i <= (value * 10f + 0.001f)
                    drawLine(
                        color = if (active) accent.copy(alpha = 0.65f) else Cream.copy(alpha = 0.14f),
                        start = center + direction * (outerRadius - 3.5.dp.toPx()),
                        end = center + direction * outerRadius,
                        strokeWidth = 1.1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    i++
                }

                // Brushed metal rim.
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF2C2A31), SteelDark, Color(0xFF5F5C66), SteelDark,
                            Color(0xFF2C2A31), Color(0xFF5F5C66), Color(0xFF2C2A31)
                        ),
                        center
                    ),
                    radius = bodyRadius + 1.5.dp.toPx()
                )
                // Knob face.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B3944), Color(0xFF211F27), Color(0xFF0B0A0E)),
                        center = center + Offset(-bodyRadius * 0.32f, -bodyRadius * 0.38f),
                        radius = bodyRadius * 1.7f
                    ),
                    radius = bodyRadius
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = bodyRadius,
                    style = Stroke(1.dp.toPx())
                )

                // Value arc on the rim.
                val arcTopLeft = Offset(center.x - bodyRadius, center.y - bodyRadius)
                val arcSize = Size(bodyRadius * 2f, bodyRadius * 2f)
                drawArc(
                    color = Cream.copy(alpha = 0.10f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = accent,
                    startAngle = startAngle,
                    sweepAngle = sweep * value.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Pointer.
                val pointerAngle = Math.toRadians((startAngle + sweep * value.coerceIn(0f, 1f)).toDouble()).toFloat()
                val pointerDir = Offset(cos(pointerAngle), sin(pointerAngle))
                drawLine(
                    color = Cream,
                    start = center + pointerDir * (bodyRadius * 0.28f),
                    end = center + pointerDir * (bodyRadius * 0.80f),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.9f),
                    start = center + pointerDir * (bodyRadius * 0.84f),
                    end = center + pointerDir * (bodyRadius * 0.96f),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color(0xFF17161C), radius = bodyRadius * 0.27f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = bodyRadius * 0.95f,
                    style = Stroke(1f)
                )
            }

            if (dragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                        .background(Color(0xF21A1510), RoundedCornerShape(3.dp))
                        .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = valueText,
                        color = AmberBright,
                        fontFamily = ReadoutFont,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(1.dp))
        Text(
            text = valueText,
            color = if (dragging) accent else CreamFaint,
            fontFamily = ReadoutFont,
            fontSize = 7.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
