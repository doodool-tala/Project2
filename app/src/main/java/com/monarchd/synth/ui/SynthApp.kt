package com.monarchd.synth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector as VectorIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.monarchd.synth.SynthViewModel
import com.monarchd.synth.model.Patch
import com.monarchd.synth.ui.components.LedIndicator
import com.monarchd.synth.ui.components.LogoMark
import com.monarchd.synth.ui.components.LocalHapticsEnabled
import com.monarchd.synth.ui.components.ScopeStrip
import com.monarchd.synth.ui.components.SynthKeyboard
import com.monarchd.synth.ui.components.WheelRack
import com.monarchd.synth.ui.theme.Amber
import com.monarchd.synth.ui.theme.AmberDeep
import com.monarchd.synth.ui.theme.Cream
import com.monarchd.synth.ui.theme.CreamDim
import com.monarchd.synth.ui.theme.CreamFaint
import com.monarchd.synth.ui.theme.DisplayFont
import com.monarchd.synth.ui.theme.Ink
import com.monarchd.synth.ui.theme.PanelEdge
import com.monarchd.synth.ui.theme.PanelFont
import com.monarchd.synth.ui.theme.PanelInset
import com.monarchd.synth.ui.theme.ReadoutFont
import com.monarchd.synth.ui.theme.Wood
import com.monarchd.synth.ui.theme.WoodDark
import com.monarchd.synth.ui.theme.WoodLight

@Composable
fun SynthApp(viewModel: SynthViewModel, enableAudio: Boolean = true) {
    var showLibrary by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var pressedNotes by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var octaveShift by rememberSaveable { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    if (enableAudio) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.startAudio()
                    Lifecycle.Event.ON_STOP -> {
                        viewModel.stopAudio()
                        pressedNotes = emptySet()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                viewModel.startAudio()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewModel.stopAudio()
            }
        }
    }

    val settings = viewModel.settings

    CompositionLocalProvider(LocalHapticsEnabled provides settings.haptics) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
            val landscape = maxWidth > maxHeight
            val tablet = maxWidth >= 1000.dp
            val compactChrome = landscape && !tablet
            val metrics = when {
                tablet -> PanelMetrics(knob = 60.dp, largeKnob = 82.dp, graphHeight = 96.dp, compact = false, expanded = true)
                landscape -> PanelMetrics(knob = 42.dp, largeKnob = 58.dp, graphHeight = 54.dp, compact = true)
                else -> PanelMetrics(knob = 50.dp, largeKnob = 72.dp, graphHeight = 78.dp, compact = false)
            }
            val keyboardHeight = when {
                tablet -> 128.dp
                landscape && maxHeight < 430.dp -> 78.dp
                landscape -> 92.dp
                maxHeight < 700.dp -> 108.dp
                else -> 128.dp
            }
            val keyCount = if (maxWidth >= 600.dp) 37 else 25
            val baseMidi = 48 + octaveShift * 12
            val soundActive = pressedNotes.isNotEmpty()

            InstrumentBackground(landscape, Modifier.fillMaxSize())

            CompositionLocalProvider(LocalPanelMetrics provides metrics) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(horizontal = if (landscape) 16.dp else 10.dp)
                ) {
                    TopBar(
                        patch = viewModel.patch,
                        active = soundActive,
                        compact = compactChrome,
                        onOpenLibrary = { showLibrary = true },
                        onOpenSettings = { showSettings = true },
                        onOpenHelp = { showHelp = true }
                    )
                    PanelPager(
                        patch = viewModel.patch,
                        onPatch = { newPatch -> viewModel.updatePatch { newPatch } },
                        onRandomize = viewModel::randomize,
                        onPanic = viewModel::panic,
                        soundActive = soundActive,
                        copyScope = viewModel::copyScope,
                        tabHeight = when {
                            tablet -> 40.dp
                            compactChrome -> 32.dp
                            else -> 34.dp
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BottomDock(
                        audioActive = soundActive,
                        copyScope = viewModel::copyScope,
                        compactChrome = compactChrome,
                        keyHeight = keyboardHeight,
                        keyCount = keyCount,
                        baseMidi = baseMidi,
                        octaveShift = octaveShift,
                        onOctaveShift = { delta ->
                            octaveShift = (octaveShift + delta).coerceIn(-2, 2)
                        },
                        pressedNotes = pressedNotes,
                        showLabels = settings.showKeyLabels,
                        onNoteOn = { note ->
                            pressedNotes = pressedNotes + note
                            viewModel.noteOn(note)
                        },
                        onNoteOff = { note ->
                            pressedNotes = pressedNotes - note
                            viewModel.noteOff(note)
                        },
                        onPitchBend = viewModel::setPitchBend,
                        onModWheel = viewModel::setModWheel
                    )
                }
            }

            if (viewModel.showIntro) {
                IntroOverlay(onEnter = viewModel::dismissIntro)
            }
        }

        if (showLibrary) {
            PatchLibrarySheet(
                factory = viewModel.factoryPresets,
                userPresets = viewModel.userPresets,
                current = viewModel.patch,
                onLoad = { preset ->
                    viewModel.loadPatch(preset.patch)
                    pressedNotes = emptySet()
                    showLibrary = false
                },
                onSave = { name -> viewModel.saveAs(name) },
                onDelete = { name -> viewModel.deletePreset(name) },
                onRandomize = {
                    viewModel.randomize()
                    pressedNotes = emptySet()
                    showLibrary = false
                },
                onDismiss = { showLibrary = false }
            )
        }
        if (showSettings) {
            SettingsSheet(
                settings = settings,
                audioSummary = viewModel.audioSummary,
                onChange = { transform -> viewModel.updateSettings(transform) },
                onPanic = viewModel::panic,
                onDismiss = { showSettings = false }
            )
        }
        if (showHelp) {
            HelpSheet(onDismiss = { showHelp = false })
        }
    }
}

@Composable
private fun TopBar(
    patch: Patch,
    active: Boolean,
    compact: Boolean,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 46.dp else 48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoMark(Modifier.size(23.dp))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.width(94.dp)) {
                Text(
                    text = "MONARCH",
                    color = Cream,
                    fontFamily = DisplayFont,
                    fontSize = 16.sp,
                    letterSpacing = 2.4.sp,
                    maxLines = 1
                )
                Text(
                    text = "SERIES D",
                    color = Amber,
                    fontFamily = PanelFont,
                    fontSize = 6.sp,
                    letterSpacing = 2.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(6.dp))
            PresetChip(
                patch = patch,
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onOpenLibrary
            )
            Spacer(Modifier.width(5.dp))
            LedIndicator(active, Modifier.size(5.dp))
            Spacer(Modifier.width(2.dp))
            TopIcon(Icons.Outlined.Info, "How to play", compact, onOpenHelp)
            TopIcon(Icons.Outlined.Settings, "Settings", compact, onOpenSettings)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, PanelEdge.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun PresetChip(
    patch: Patch,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(if (compact) 36.dp else 40.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(PanelInset)
            .border(1.dp, PanelEdge.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Preset ${patch.name}. Open the patch library."
                role = Role.Button
            }
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = patch.name.uppercase(),
                color = Cream,
                fontFamily = PanelFont,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                letterSpacing = 0.9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${patch.category.label} · ${patch.activeOscillators} OSC",
                color = Amber,
                fontFamily = ReadoutFont,
                fontSize = 6.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
        Text(
            text = "▾",
            color = CreamDim,
            fontFamily = PanelFont,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun TopIcon(
    icon: VectorIcon,
    description: String,
    compact: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (compact) 40.dp else 44.dp)
            .clip(RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CreamDim,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun BottomDock(
    audioActive: Boolean,
    copyScope: (FloatArray) -> Unit,
    compactChrome: Boolean,
    keyHeight: Dp,
    keyCount: Int,
    baseMidi: Int,
    octaveShift: Int,
    onOctaveShift: (Int) -> Unit,
    pressedNotes: Set<Int>,
    showLabels: Boolean,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    onPitchBend: (Float) -> Unit,
    onModWheel: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactChrome) 28.dp else 32.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SIGNAL",
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 6.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.width(6.dp))
            ScopeStrip(
                active = audioActive,
                copyScope = copyScope,
                modifier = Modifier
                    .weight(1f)
                    .height(if (compactChrome) 16.dp else 18.dp)
            )
            Spacer(Modifier.width(10.dp))
            OctaveStepper(
                octaveShift = octaveShift,
                compact = compactChrome,
                onShift = onOctaveShift
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(keyHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            WheelRack(
                onPitchBend = onPitchBend,
                onModWheel = onModWheel,
                modifier = Modifier
                    .width(70.dp)
                    .height(keyHeight)
            )
            Spacer(Modifier.width(8.dp))
            SynthKeyboard(
                baseMidi = baseMidi,
                keyCount = keyCount,
                pressedNotes = pressedNotes,
                showLabels = showLabels,
                onNoteOn = onNoteOn,
                onNoteOff = onNoteOff,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun OctaveStepper(octaveShift: Int, compact: Boolean, onShift: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", enabled = octaveShift > -2, compact = compact) { onShift(-1) }
        Text(
            text = "C${3 + octaveShift}",
            color = CreamDim,
            fontFamily = ReadoutFont,
            fontSize = 7.5.sp,
            letterSpacing = 0.3.sp,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
        StepButton("+", enabled = octaveShift < 2, compact = compact) { onShift(1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, compact: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (compact) 26.dp else 30.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(PanelInset)
            .border(1.dp, PanelEdge.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = if (label == "+") "Octave up" else "Octave down"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) CreamDim else CreamFaint.copy(alpha = 0.3f),
            fontFamily = PanelFont,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun InstrumentBackground(landscape: Boolean, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF120D09), Ink, Color(0xFF040303))))

        val cheek = if (landscape) 26.dp.toPx() else 18.dp.toPx()
        var seed = 99_173L
        fun random(): Float {
            seed = (seed * 1_103_515_245L + 12_345L) and 0x7FFF_FFFF
            return ((seed shr 9) % 1_000L).toFloat() / 1_000f
        }

        // Brushed center panel.
        var line = 0
        while (line < 130) {
            val y = size.height * line / 130f
            drawLine(
                color = Color.White.copy(alpha = 0.012f + random() * 0.022f),
                start = Offset(cheek, y),
                end = Offset(size.width - cheek, y),
                strokeWidth = 1f
            )
            line++
        }

        // Wooden cheeks, cut from one billet and lit from the centre.
        val woodBrush = Brush.horizontalGradient(
            0f to Color(0xFF060402),
            0.20f to WoodDark,
            0.46f to Wood,
            0.64f to WoodLight,
            0.82f to Wood,
            1f to Color(0xFF060402)
        )
        drawRect(brush = woodBrush, topLeft = Offset.Zero, size = Size(cheek, size.height))
        drawRect(brush = woodBrush, topLeft = Offset(size.width - cheek, 0f), size = Size(cheek, size.height))
        var side = 0
        while (side < 2) {
            val baseX = if (side == 0) 0f else size.width - cheek
            var grain = 0
            while (grain < 90) {
                val x = baseX + random() * cheek
                val pale = random() > 0.62f
                drawLine(
                    color = if (pale) {
                        Color(0xFF9C6538).copy(alpha = 0.04f + random() * 0.09f)
                    } else {
                        Color(0xFF0C0502).copy(alpha = 0.10f + random() * 0.16f)
                    },
                    start = Offset(x, 0f),
                    end = Offset(x + (random() - 0.5f) * 3f, size.height),
                    strokeWidth = 0.8f
                )
                grain++
            }
            val glowX = baseX + cheek * 0.62f
            drawLine(
                color = Cream.copy(alpha = 0.035f),
                start = Offset(glowX, 0f),
                end = Offset(glowX, size.height),
                strokeWidth = 0.9f
            )
            val edge = if (side == 0) cheek else size.width - cheek
            drawLine(
                color = Color(0xFFD0A35E).copy(alpha = 0.34f),
                start = Offset(edge, 0f),
                end = Offset(edge, size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.65f),
                start = Offset(edge + (if (side == 0) 2f else -2f), 0f),
                end = Offset(edge + (if (side == 0) 2f else -2f), size.height),
                strokeWidth = 2f
            )
            side++
        }

        // Vignette and top/bottom falloff.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f)),
                center = Offset(size.width / 2f, size.height * 0.44f),
                radius = size.maxDimension * 0.74f
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.4f))
            )
        )
        drawRect(
            color = Cream.copy(alpha = 0.02f),
            topLeft = Offset(cheek + 1f, 0f),
            size = Size(size.width - cheek * 2f - 2f, size.height),
            style = Stroke(1f)
        )
    }
}
