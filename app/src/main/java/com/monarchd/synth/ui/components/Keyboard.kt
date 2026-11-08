package com.monarchd.synth.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monarchd.synth.ui.theme.AmberBright
import com.monarchd.synth.ui.theme.Cream
import com.monarchd.synth.ui.theme.CreamDim
import com.monarchd.synth.ui.theme.CreamFaint
import com.monarchd.synth.ui.theme.KeyBlack
import com.monarchd.synth.ui.theme.KeyBlackHi
import com.monarchd.synth.ui.theme.KeyWhite
import com.monarchd.synth.ui.theme.KeyWhiteShade
import com.monarchd.synth.ui.theme.Oxblood
import com.monarchd.synth.ui.theme.PanelFont
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class KeyShape(
    val note: Int,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val black: Boolean
)

private fun isWhiteNote(note: Int): Boolean = when (((note % 12) + 12) % 12) {
    0, 2, 4, 5, 7, 9, 11 -> true
    else -> false
}

private fun buildKeyShapes(
    width: Float,
    height: Float,
    baseMidi: Int,
    keyCount: Int
): List<KeyShape> {
    if (width <= 0f || height <= 0f) return emptyList()
    val notes = (0 until keyCount).map { baseMidi + it }
    val whiteNotes = notes.filter { isWhiteNote(it) }
    if (whiteNotes.isEmpty()) return emptyList()
    val whiteWidth = width / whiteNotes.size
    val felt = height * 0.055f
    val whiteTop = felt
    val whiteBottom = height
    val shapes = ArrayList<KeyShape>(notes.size)

    whiteNotes.forEachIndexed { index, note ->
        shapes += KeyShape(
            note = note,
            left = index * whiteWidth,
            right = (index + 1) * whiteWidth,
            top = whiteTop,
            bottom = whiteBottom,
            black = false
        )
    }

    val blackWidth = whiteWidth * 0.60f
    val blackBottom = whiteTop + (whiteBottom - whiteTop) * 0.62f
    notes.filterNot { isWhiteNote(it) }.forEach { note ->
        val whitesBefore = notes.count { it < note && isWhiteNote(it) }
        val boundary = whitesBefore * whiteWidth
        shapes += KeyShape(
            note = note,
            left = boundary - blackWidth / 2f,
            right = boundary + blackWidth / 2f,
            top = whiteTop,
            bottom = blackBottom,
            black = true
        )
    }
    return shapes
}

private fun noteAt(keys: List<KeyShape>, x: Float, y: Float): Int? {
    var i = keys.size - 1
    while (i >= 0) {
        val key = keys[i]
        if (x >= key.left && x < key.right && y >= key.top && y < key.bottom) return key.note
        i--
    }
    return null
}

/**
 * A 25 or 37 key keyboard drawn in a single Canvas. Pointer tracking supports
 * multi-touch chords and true glissando across the keys, including black keys.
 */
@Composable
fun SynthKeyboard(
    baseMidi: Int,
    keyCount: Int,
    pressedNotes: Set<Int>,
    showLabels: Boolean,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticsEnabled = LocalHapticsEnabled.current
    val haptic = LocalHapticFeedback.current
    val latestOn = rememberUpdatedState(onNoteOn)
    val latestOff = rememberUpdatedState(onNoteOff)
    val latestLabels = rememberUpdatedState(showLabels)

    val labelPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)
            color = android.graphics.Color.argb(155, 62, 47, 30)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF070609))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val keys = remember(baseMidi, keyCount, widthPx, heightPx) {
            buildKeyShapes(widthPx, heightPx, baseMidi, keyCount)
        }

        Canvas(Modifier.fillMaxSize()) {
            val feltHeight = size.height * 0.055f
            drawRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF5E2321), Oxblood, Color(0xFF2A0E0D))),
                topLeft = Offset.Zero,
                size = Size(size.width, feltHeight)
            )
            drawLine(
                color = Color(0xFF7B3B33).copy(alpha = 0.6f),
                start = Offset(0f, feltHeight),
                end = Offset(size.width, feltHeight),
                strokeWidth = 1f
            )

            keys.forEach { key ->
                val pressed = pressedNotes.contains(key.note)
                if (!key.black) {
                    val keyWidth = key.right - key.left
                    drawRect(
                        brush = if (pressed) {
                            Brush.verticalGradient(
                                listOf(Color(0xFFE9CFA4), Color(0xFFC3A26F)),
                                startY = key.top,
                                endY = key.bottom
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(KeyWhite, KeyWhiteShade),
                                startY = key.top,
                                endY = key.bottom
                            )
                        },
                        topLeft = Offset(key.left, key.top),
                        size = Size(keyWidth - 1f, key.bottom - key.top)
                    )
                    drawLine(
                        color = Color(0xFF3A2E22).copy(alpha = 0.45f),
                        start = Offset(key.left, key.top),
                        end = Offset(key.left, key.bottom),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = if (pressed) 0.12f else 0.38f),
                        start = Offset(key.left + 1f, key.top + 1f),
                        end = Offset(key.right - 1f, key.top + 1f),
                        strokeWidth = 1f
                    )
                    drawRect(
                        color = Color(0xFF1C130D).copy(alpha = 0.30f),
                        topLeft = Offset(key.left + 1f, key.bottom - 2.dp.toPx()),
                        size = Size(keyWidth - 2f, 2.dp.toPx())
                    )
                }
            }

            keys.forEach { key ->
                if (!key.black) return@forEach
                val pressed = pressedNotes.contains(key.note)
                val width = key.right - key.left
                val height = key.bottom - key.top
                // Soft shadow under each sharp.
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(key.left - 1.dp.toPx(), key.top + 1.5.dp.toPx()),
                    size = Size(width + 2.dp.toPx(), height + 2.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                drawRoundRect(
                    brush = if (pressed) {
                        Brush.verticalGradient(listOf(Color(0xFF7A5624), Color(0xFF2D1D0C)))
                    } else {
                        Brush.verticalGradient(
                            listOf(KeyBlackHi, KeyBlack, Color(0xFF050407)),
                            startY = key.top,
                            endY = key.bottom
                        )
                    },
                    topLeft = Offset(key.left, key.top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                drawLine(
                    color = if (pressed) AmberBright.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.16f),
                    start = Offset(key.left + 2.dp.toPx(), key.top + 1.dp.toPx()),
                    end = Offset(key.right - 2.dp.toPx(), key.top + 1.dp.toPx()),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.6f),
                    start = Offset(key.left + 1.dp.toPx(), key.bottom - 1.dp.toPx()),
                    end = Offset(key.right - 1.dp.toPx(), key.bottom - 1.dp.toPx()),
                    strokeWidth = 1f
                )
            }

            if (latestLabels.value) {
                labelPaint.textSize = 8.5.sp.toPx()
                keys.forEach { key ->
                    if (!key.black && key.note % 12 == 0) {
                        val label = "C${key.note / 12 - 1}"
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            (key.left + key.right) / 2f,
                            key.bottom - 4.dp.toPx(),
                            labelPaint
                        )
                    }
                }
            }

            // Bottom rail.
            drawRect(
                color = Color(0xFF09080B),
                topLeft = Offset(0f, size.height - 1.5.dp.toPx()),
                size = Size(size.width, 1.5.dp.toPx())
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(baseMidi, keyCount, widthPx, heightPx, hapticsEnabled) {
                    awaitPointerEventScope {
                        val active = mutableMapOf<PointerId, Int>()
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val present = HashSet<PointerId>()
                                for (change in event.changes) {
                                    present.add(change.id)
                                    if (change.pressed) {
                                        val note = noteAt(keys, change.position.x, change.position.y)
                                        val previous = active[change.id]
                                        if (note != null && note != previous) {
                                            if (previous != null) latestOff.value(previous)
                                            active[change.id] = note
                                            latestOn.value(note)
                                            if (hapticsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        } else if (note == null && previous != null) {
                                            latestOff.value(previous)
                                            active.remove(change.id)
                                        }
                                        change.consume()
                                    } else {
                                        active.remove(change.id)?.let { latestOff.value(it) }
                                        change.consume()
                                    }
                                }
                                if (present.size < active.size) {
                                    val iterator = active.entries.iterator()
                                    while (iterator.hasNext()) {
                                        val entry = iterator.next()
                                        if (entry.key !in present) {
                                            latestOff.value(entry.value)
                                            iterator.remove()
                                        }
                                    }
                                }
                            }
                        } finally {
                            active.values.forEach { latestOff.value(it) }
                            active.clear()
                        }
                    }
                }
        )
    }
}

@Composable
fun WheelRack(
    onPitchBend: (Float) -> Unit,
    onModWheel: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PitchWheel(
            onBend = onPitchBend,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        ModWheel(
            onWheel = onModWheel,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun PitchWheel(onBend: (Float) -> Unit, modifier: Modifier = Modifier) {
    var value by remember { mutableFloatStateOf(0f) }
    var springJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val latestBend = rememberUpdatedState(onBend)

    LaunchedEffect(Unit) { latestBend.value(0f) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { springJob?.cancel() },
                        onDragEnd = {
                            springJob = scope.launch {
                                animate(
                                    initialValue = value,
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) { v, _ ->
                                    value = v
                                    latestBend.value(v)
                                }
                            }
                        },
                        onDragCancel = {
                            springJob = scope.launch {
                                animate(
                                    initialValue = value,
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) { v, _ ->
                                    value = v
                                    latestBend.value(v)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / size.height * 2f
                        value = (value + delta).coerceIn(-1f, 1f)
                        latestBend.value(value)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawWheelBody((value + 1f) / 2f, isPitch = true)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "PITCH",
            color = CreamFaint,
            fontFamily = PanelFont,
            fontWeight = FontWeight.Medium,
            fontSize = 6.5.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ModWheel(onWheel: (Float) -> Unit, modifier: Modifier = Modifier) {
    var value by remember { mutableFloatStateOf(0f) }
    val latestWheel = rememberUpdatedState(onWheel)

    LaunchedEffect(Unit) { latestWheel.value(0f) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / size.height
                        value = (value + delta).coerceIn(0f, 1f)
                        latestWheel.value(value)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawWheelBody(value, isPitch = false)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "MOD",
            color = CreamFaint,
            fontFamily = PanelFont,
            fontWeight = FontWeight.Medium,
            fontSize = 6.5.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private fun DrawScope.drawWheelBody(value01: Float, isPitch: Boolean) {
    val w = size.width
    val h = size.height
    val inset = 2.dp.toPx()
    val top = 4.dp.toPx()
    val bottom = h - 4.dp.toPx()
    val radius = 6.dp.toPx()

    // Recessed well around the wheel.
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF030204), Color(0xFF0E0C09), Color(0xFF030204))
        ),
        topLeft = Offset.Zero,
        size = Size(w, h),
        cornerRadius = CornerRadius(6.dp.toPx())
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(0.5f, 0.5f),
        size = Size(w - 1f, h - 1f),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(1f)
    )
    // Shadow cast by the faceplate onto the well.
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.65f),
            0.10f to Color.Transparent
        ),
        topLeft = Offset(inset, top),
        size = Size(w - inset * 2f, bottom - top),
        cornerRadius = CornerRadius(radius)
    )

    // Cylindrical rubber wheel, lit across its width.
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0f to Color(0xFF14110E),
            0.18f to Color(0xFF3A342C),
            0.50f to Color(0xFF6A6152),
            0.72f to Color(0xFF3B342B),
            1f to Color(0xFF15110D)
        ),
        topLeft = Offset(inset, top),
        size = Size(w - inset * 2f, bottom - top),
        cornerRadius = CornerRadius(radius)
    )

    val ribs = 18
    var i = 1
    while (i < ribs) {
        val y = top + (bottom - top) * i / ribs
        drawLine(Color.Black.copy(alpha = 0.30f), Offset(inset, y), Offset(w - inset, y), 1f)
        drawLine(
            color = Color.White.copy(alpha = 0.045f),
            start = Offset(inset, y + 0.9.dp.toPx()),
            end = Offset(w - inset, y + 0.9.dp.toPx()),
            strokeWidth = 0.8f
        )
        i++
    }

    // Curvature at the top and bottom of the wheel.
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.55f),
            0.16f to Color.Transparent,
            0.84f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.60f)
        ),
        topLeft = Offset(inset, top),
        size = Size(w - inset * 2f, bottom - top),
        cornerRadius = CornerRadius(radius)
    )
    drawLine(
        color = Color.White.copy(alpha = 0.10f),
        start = Offset(inset + 0.8.dp.toPx(), top + radius),
        end = Offset(inset + 0.8.dp.toPx(), bottom - radius),
        strokeWidth = 1f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.05f),
        start = Offset(w - inset - 1.dp.toPx(), top + radius),
        end = Offset(w - inset - 1.dp.toPx(), bottom - radius),
        strokeWidth = 1f
    )

    val centerY = bottom - value01.coerceIn(0f, 1f) * (bottom - top)
    if (isPitch) {
        val mid = (top + bottom) / 2f
        drawLine(
            color = Cream.copy(alpha = 0.22f),
            start = Offset(inset, mid),
            end = Offset(w - inset, mid),
            strokeWidth = 1f
        )
        drawLine(
            color = Cream.copy(alpha = 0.38f),
            start = Offset(0.6.dp.toPx(), mid),
            end = Offset(1.6.dp.toPx(), mid),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = Cream.copy(alpha = 0.38f),
            start = Offset(w - 1.6.dp.toPx(), mid),
            end = Offset(w - 0.6.dp.toPx(), mid),
            strokeWidth = 1.2.dp.toPx()
        )
    }

    // Amber index line with a soft phosphor glow.
    listOf(
        5.dp.toPx() to 0.10f,
        3.dp.toPx() to 0.22f,
        1.2.dp.toPx() to 0.95f
    ).forEach { (thickness, alpha) ->
        drawLine(
            color = Color(0xFFFFB84D).copy(alpha = alpha),
            start = Offset(inset + 1.dp.toPx(), centerY),
            end = Offset(w - inset - 1.dp.toPx(), centerY),
            strokeWidth = thickness,
            cap = StrokeCap.Round
        )
    }
    drawLine(
        color = Color(0xFFFFE7BB),
        start = Offset(inset + 3.dp.toPx(), centerY),
        end = Offset(w - inset - 3.dp.toPx(), centerY),
        strokeWidth = 0.7.dp.toPx(),
        cap = StrokeCap.Round
    )
}
