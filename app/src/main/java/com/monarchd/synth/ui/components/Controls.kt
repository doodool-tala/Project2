package com.monarchd.synth.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monarchd.synth.audio.SCOPE_SIZE
import com.monarchd.synth.model.OscWave
import com.monarchd.synth.ui.Fmt
import com.monarchd.synth.ui.theme.Amber
import com.monarchd.synth.ui.theme.AmberBright
import com.monarchd.synth.ui.theme.AmberGlow
import com.monarchd.synth.ui.theme.Cream
import com.monarchd.synth.ui.theme.CreamDim
import com.monarchd.synth.ui.theme.CreamFaint
import com.monarchd.synth.ui.theme.LedOff
import com.monarchd.synth.ui.theme.Panel
import com.monarchd.synth.ui.theme.PanelEdge
import com.monarchd.synth.ui.theme.PanelFont
import com.monarchd.synth.ui.theme.PanelHigh
import com.monarchd.synth.ui.theme.PanelInset
import com.monarchd.synth.ui.theme.ReadoutFont
import com.monarchd.synth.ui.theme.SteelDark
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

val LocalHapticsEnabled = androidx.compose.runtime.compositionLocalOf { true }

/** Engraved section plate used for each functional block of the panel. */
@Composable
fun SectionPlate(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(listOf(PanelHigh, Panel), endY = 700f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, PanelEdge.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .drawWithContent {
                drawContent()
                drawLine(
                    color = Color.White.copy(alpha = 0.045f),
                    start = Offset(6.dp.toPx(), 1f),
                    end = Offset(size.width - 6.dp.toPx(), 1f),
                    strokeWidth = 1f
                )
            }
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(9.dp)
                    .background(Amber)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title.uppercase(),
                color = CreamDim,
                fontFamily = PanelFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.5.sp,
                letterSpacing = 1.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = subtitle.uppercase(),
                    color = CreamFaint,
                    fontFamily = PanelFont,
                    fontSize = 6.5.sp,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        content()
    }
}

@Composable
fun EngravedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    if (accent) {
                        listOf(Color(0xFF3A2A14), Color(0xFF22180C))
                    } else {
                        listOf(Color(0xFF26242B), Color(0xFF141317))
                    }
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (accent) Amber.copy(alpha = 0.65f) else PanelEdge.copy(alpha = 0.8f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (accent) AmberBright else CreamDim,
            fontFamily = PanelFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.5.sp,
            letterSpacing = 1.3.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WaveformSelector(
    selected: OscWave,
    onSelect: (OscWave) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        OscWave.entries.forEach { wave ->
            val isSelected = wave == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .background(
                        if (isSelected) Amber.copy(alpha = 0.15f) else Color(0xFF111015),
                        RoundedCornerShape(2.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Amber.copy(alpha = 0.65f) else PanelEdge.copy(alpha = 0.55f),
                        RoundedCornerShape(2.dp)
                    )
                    .selectable(selected = isSelected, onClick = { onSelect(wave) }, role = Role.RadioButton)
                    .semantics { contentDescription = wave.longLabel }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                WaveIcon(
                    wave = wave,
                    color = if (isSelected) AmberBright else CreamDim,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun WaveIcon(wave: OscWave, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val top = h * 0.16f
        val bottom = h * 0.84f
        val path = Path()
        when (wave) {
            OscWave.TRIANGLE -> {
                path.moveTo(0f, bottom)
                path.lineTo(w * 0.5f, top)
                path.lineTo(w, bottom)
            }
            OscWave.TRI_SAW -> {
                path.moveTo(0f, bottom)
                path.lineTo(w * 0.72f, top)
                path.lineTo(w, bottom * 0.75f)
            }
            OscWave.SAWTOOTH -> {
                path.moveTo(0f, bottom)
                path.lineTo(w * 0.9f, top)
                path.lineTo(w * 0.9f, bottom)
            }
            OscWave.SQUARE -> {
                path.moveTo(0f, bottom)
                path.lineTo(0f, top)
                path.lineTo(w * 0.5f, top)
                path.lineTo(w * 0.5f, bottom)
                path.lineTo(w, bottom)
                path.lineTo(w, top)
            }
            OscWave.PULSE_WIDE -> {
                path.moveTo(0f, bottom)
                path.lineTo(0f, top)
                path.lineTo(w * 0.34f, top)
                path.lineTo(w * 0.34f, bottom)
                path.lineTo(w, bottom)
                path.lineTo(w, top)
            }
            OscWave.PULSE_NARROW -> {
                path.moveTo(0f, bottom)
                path.lineTo(0f, top)
                path.lineTo(w * 0.18f, top)
                path.lineTo(w * 0.18f, bottom)
                path.lineTo(w, bottom)
                path.lineTo(w, top)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = true
) {
    if (horizontal) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(PanelInset, RoundedCornerShape(3.dp))
                .border(1.dp, PanelEdge.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(19.dp)
                        .background(
                            if (selected) Brush.verticalGradient(
                                listOf(Amber.copy(alpha = 0.20f), Amber.copy(alpha = 0.06f))
                            ) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.RadioButton)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(0.55f)
                                .height(1.dp)
                                .background(AmberBright)
                        )
                    }
                    Text(
                        text = label,
                        color = if (selected) AmberBright else CreamFaint,
                        fontFamily = PanelFont,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 8.sp,
                        letterSpacing = 0.4.sp,
                        maxLines = 1
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .background(PanelInset, RoundedCornerShape(3.dp))
                .border(1.dp, PanelEdge.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(17.dp)
                        .background(if (selected) Amber.copy(alpha = 0.12f) else Color.Transparent)
                        .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.RadioButton)
                        .padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(3.dp)
                            .background(if (selected) AmberBright else Color(0xFF34323A), CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = if (selected) Cream else CreamFaint,
                        fontFamily = PanelFont,
                        fontSize = 7.5.sp,
                        letterSpacing = 0.6.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun LabeledToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(PanelInset, RoundedCornerShape(3.dp))
            .border(1.dp, PanelEdge.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 22.dp, height = 11.dp)
                .background(
                    if (checked) Amber.copy(alpha = 0.35f) else Color(0xFF26242B),
                    CircleShape
                )
                .border(1.dp, if (checked) Amber.copy(alpha = 0.7f) else PanelEdge, CircleShape)
        ) {
            Box(
                Modifier
                    .padding(1.5.dp)
                    .size(8.dp)
                    .background(if (checked) AmberBright else SteelDark, CircleShape)
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label.uppercase(),
            color = if (checked) Cream else CreamDim,
            fontFamily = PanelFont,
            fontSize = 7.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
    }
}

@Composable
fun LedIndicator(active: Boolean, modifier: Modifier = Modifier) {
    val glow by animateFloatAsState(if (active) 1f else 0.18f, label = "ledGlow")
    Canvas(modifier) {
        val radius = min(size.width, size.height) / 2f
        drawCircle(color = AmberGlow.copy(alpha = 0.55f * glow), radius = radius * 2.1f)
        drawCircle(color = if (active) AmberBright else LedOff, radius = radius)
        drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = radius, style = Stroke(0.8.dp.toPx()))
    }
}

/** Tiny moving waveform used above the keyboard. */
@Composable
fun ScopeStrip(
    active: Boolean,
    copyScope: (FloatArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val buffer = remember { FloatArray(SCOPE_SIZE) }
    val latestCopy by rememberUpdatedState(copyScope)
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            latestCopy(buffer)
            frame++
        }
    }

    Canvas(
        modifier
            .background(Color(0xFF08070A), RoundedCornerShape(2.dp))
            .border(1.dp, PanelEdge.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
    ) {
        val mid = size.height / 2f
        drawLine(
            color = Cream.copy(alpha = 0.06f),
            start = Offset(0f, mid),
            end = Offset(size.width, mid),
            strokeWidth = 1f
        )
        val currentFrame = frame
        if (active) {
            val path = Path()
            var i = 0
            while (i < buffer.size) {
                val x = i / (buffer.size - 1f) * size.width
                val y = mid - buffer[i].coerceIn(-1.2f, 1.2f) * mid * 0.82f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                i++
            }
            drawPath(path, color = Amber.copy(alpha = 0.9f), style = Stroke(1.2.dp.toPx()))
            // A very subtle moving scanline keeps the display alive.
            val scanY = ((currentFrame * 2) % (size.height.toInt() + 1)).toFloat()
            drawLine(
                color = Amber.copy(alpha = 0.05f),
                start = Offset(0f, scanY),
                end = Offset(size.width, scanY),
                strokeWidth = 1f
            )
        } else {
            drawLine(
                color = Amber.copy(alpha = 0.22f),
                start = Offset(size.width * 0.04f, mid),
                end = Offset(size.width * 0.96f, mid),
                strokeWidth = 1f
            )
        }
    }
}

/** Small peak meter for the output plate. */
@Composable
fun OutputMeter(
    active: Boolean,
    copyScope: (FloatArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val buffer = remember { FloatArray(SCOPE_SIZE) }
    val levels = remember { FloatArray(9) }
    val latestCopy by rememberUpdatedState(copyScope)
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(active) {
        if (!active) {
            levels.fill(0f)
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            latestCopy(buffer)
            var sum = 0f
            buffer.forEach { sum += it * it }
            val rms = sqrt(sum / buffer.size)
            var i = 0
            while (i < levels.size) {
                val target = if (i == 0) (rms * 2.6f).coerceIn(0f, 1f) else levels[i - 1] * 0.93f
                levels[i] = maxOf(levels[i] * 0.82f, target)
                i++
            }
            frame++
        }
    }

    Canvas(modifier) {
        val tick = frame
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (levels.size - 1)) / levels.size
        var i = 0
        while (i < levels.size) {
            val left = i * (barWidth + gap)
            val top = size.height * (1f - levels[i].coerceIn(0f, 1f))
            drawRect(
                color = if (levels[i] > 0.75f) AmberBright else Amber.copy(alpha = 0.8f),
                topLeft = Offset(left, top),
                size = Size(barWidth, size.height - top)
            )
            drawRect(
                color = Cream.copy(alpha = 0.05f + (tick % 2) * 0.002f),
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height)
            )
            i++
        }
    }
}

/** Approximate 4-pole ladder magnitude response with the resonant peak. */
@Composable
fun FilterResponseGraph(
    cutoffHz: Float,
    resonance: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .background(Color(0xFF0B0A0E), RoundedCornerShape(4.dp))
            .border(1.dp, PanelEdge.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
    ) {
        val w = size.width
        val h = size.height
        // Grid.
        var i = 1
        while (i < 4) {
            val x = w * i / 4f
            drawLine(Cream.copy(alpha = 0.055f), Offset(x, 0f), Offset(x, h), 1f)
            i++
        }
        i = 1
        while (i < 4) {
            val y = h * i / 4f
            drawLine(Cream.copy(alpha = 0.055f), Offset(0f, y), Offset(w, y), 1f)
            i++
        }

        val minHz = 20.0
        val maxHz = 20_000.0
        val logRange = ln(maxHz / minHz)
        fun xFor(hz: Double): Float = (ln(hz / minHz) / logRange).coerceIn(0.0, 1.0).toFloat() * w

        val fc = cutoffHz.toDouble().coerceIn(20.0, 18_000.0)
        val res = resonance.coerceIn(0f, 1f).toDouble()
        val path = Path()
        val samples = 96
        var sample = 0
        while (sample <= samples) {
            val fraction = sample / samples.toFloat()
            val hz = minHz * exp(logRange * fraction)
            val ratio = hz / fc
            val base = 1.0 / sqrt(1.0 + ratio.pow(8.0))
            val peak = 1.0 + 3.4 * res * exp(-0.5 * (ln(ratio) / 0.16).pow(2.0))
            val magnitude = (base * peak).coerceIn(0.0, 4.0)
            val db = 20.0 * log10(magnitude.coerceAtLeast(0.0001))
            val y = (h - ((db + 42.0) / 54.0).coerceIn(0.0, 1.0) * h).toFloat()
            if (sample == 0) path.moveTo(0f, y) else path.lineTo(w * fraction, y)
            sample++
        }
        drawPath(
            path,
            brush = Brush.horizontalGradient(listOf(Amber.copy(alpha = 0.55f), AmberBright)),
            style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        )

        val cutoffX = xFor(fc)
        drawLine(
            color = Cream.copy(alpha = 0.25f),
            start = Offset(cutoffX, 0f),
            end = Offset(cutoffX, h),
            strokeWidth = 1f
        )
        drawCircle(color = AmberBright, radius = 2.4.dp.toPx(), center = Offset(cutoffX, h * 0.30f))
    }
}

/** ADS(R) contour preview drawn as an amber phosphor trace. */
@Composable
fun EnvelopeGraph(
    attack: Float,
    decay: Float,
    sustain: Float,
    release: Float,
    decayEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .background(Color(0xFF0B0A0E), RoundedCornerShape(4.dp))
            .border(1.dp, PanelEdge.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
    ) {
        val w = size.width
        val h = size.height
        val pad = 3.dp.toPx()
        val usableW = w - pad * 2
        val usableH = h - pad * 2

        drawLine(
            Cream.copy(alpha = 0.06f),
            Offset(0f, pad),
            Offset(w, pad),
            1f
        )
        drawLine(
            Cream.copy(alpha = 0.06f),
            Offset(0f, h - pad),
            Offset(w, h - pad),
            1f
        )

        val attackWeight = 0.10f + attack * 0.32f
        val decayWeight = 0.10f + decay * 0.30f
        val sustainWeight = 0.22f
        val releaseWeight = 0.10f + release * 0.32f
        val total = attackWeight + decayWeight + sustainWeight + releaseWeight
        val ax = pad + usableW * attackWeight / total
        val dx = ax + usableW * decayWeight / total
        val sx = dx + usableW * sustainWeight / total
        val ex = pad + usableW
        val sustainLevel = if (decayEnabled) sustain else 1f
        val sustainY = h - pad - usableH * sustainLevel

        val line = Path()
        line.moveTo(pad, h - pad)
        line.quadraticBezierTo(pad + (ax - pad) * 0.45f, pad, ax, pad)
        line.quadraticBezierTo(ax + (dx - ax) * 0.35f, sustainY, dx, sustainY)
        line.lineTo(sx, sustainY)
        line.quadraticBezierTo(sx + (ex - sx) * 0.45f, h - pad, ex, h - pad)

        val fill = Path().apply {
            addPath(line)
            lineTo(ex, h - pad)
            lineTo(pad, h - pad)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(Amber.copy(alpha = 0.16f), Color.Transparent),
                startY = pad,
                endY = h
            )
        )
        drawPath(line, color = Amber, style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = AmberBright, radius = 1.8.dp.toPx(), center = Offset(ax, pad))
        drawCircle(color = AmberBright, radius = 1.8.dp.toPx(), center = Offset(dx, sustainY))
        drawCircle(color = AmberBright, radius = 1.8.dp.toPx(), center = Offset(ex, h - pad))
    }
}
