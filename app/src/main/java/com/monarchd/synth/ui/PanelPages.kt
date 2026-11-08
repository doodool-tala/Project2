package com.monarchd.synth.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monarchd.synth.model.KeyTrack
import com.monarchd.synth.model.Maps
import com.monarchd.synth.model.OscRange
import com.monarchd.synth.model.OscWave
import com.monarchd.synth.model.Patch
import com.monarchd.synth.ui.components.EngravedButton
import com.monarchd.synth.ui.components.EnvelopeGraph
import com.monarchd.synth.ui.components.FilterResponseGraph
import com.monarchd.synth.ui.components.LabeledToggle
import com.monarchd.synth.ui.components.LedIndicator
import com.monarchd.synth.ui.components.OutputMeter
import com.monarchd.synth.ui.components.SectionPlate
import com.monarchd.synth.ui.components.SegmentedSwitch
import com.monarchd.synth.ui.components.SynthKnob
import com.monarchd.synth.ui.components.WaveformSelector
import com.monarchd.synth.ui.theme.Amber
import com.monarchd.synth.ui.theme.AmberBright
import com.monarchd.synth.ui.theme.Cream
import com.monarchd.synth.ui.theme.CreamDim
import com.monarchd.synth.ui.theme.CreamFaint
import com.monarchd.synth.ui.theme.PanelEdge
import com.monarchd.synth.ui.theme.PanelFont
import com.monarchd.synth.ui.theme.PanelInset
import kotlinx.coroutines.launch

data class PanelMetrics(
    val knob: Dp,
    val largeKnob: Dp,
    val graphHeight: Dp,
    val compact: Boolean
)

val LocalPanelMetrics = staticCompositionLocalOf {
    PanelMetrics(knob = 48.dp, largeKnob = 68.dp, graphHeight = 72.dp, compact = false)
}

private val pageTabs = listOf("OSC", "MIX", "FILTER", "ENV", "PERF")

@Composable
fun PanelPager(
    patch: Patch,
    onPatch: (Patch) -> Unit,
    onRandomize: () -> Unit,
    onPanic: () -> Unit,
    soundActive: Boolean,
    copyScope: (FloatArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pageTabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            pageTabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        .padding(top = 3.dp, bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab,
                        color = if (selected) AmberBright else CreamFaint,
                        fontFamily = PanelFont,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 8.sp,
                        letterSpacing = 1.4.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    val underline by animateDpAsState(
                        targetValue = if (selected) 26.dp else 0.dp,
                        label = "pageUnderline"
                    )
                    Box(
                        Modifier
                            .height(1.5.dp)
                            .width(underline)
                            .background(if (selected) Amber else androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PanelEdge.copy(alpha = 0.45f))
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("panelPager"),
            pageSpacing = 8.dp
        ) { page ->
            PageScroll {
                when (page) {
                    0 -> OscillatorPage(patch, onPatch)
                    1 -> MixerPage(patch, onPatch, soundActive, copyScope)
                    2 -> FilterPage(patch, onPatch)
                    3 -> EnvelopePage(patch, onPatch)
                    else -> PerformancePage(patch, onPatch, onRandomize, onPanic)
                }
            }
        }
    }
}

@Composable
private fun PageScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 7.dp, bottom = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

// ── Oscillators ───────────────────────────────────────────────────────────────

@Composable
private fun OscillatorPage(patch: Patch, onPatch: (Patch) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 540.dp
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OscillatorCardOne(patch, onPatch, Modifier.weight(1f))
                OscillatorCardTwo(patch, onPatch, Modifier.weight(1f))
                OscillatorCardThree(patch, onPatch, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OscillatorCardOne(patch, onPatch, Modifier.fillMaxWidth())
                OscillatorCardTwo(patch, onPatch, Modifier.fillMaxWidth())
                OscillatorCardThree(patch, onPatch, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun OscillatorCardOne(patch: Patch, onPatch: (Patch) -> Unit, modifier: Modifier) {
    OscillatorCard(
        index = 1,
        wave = patch.osc1Wave,
        onWave = { onPatch(patch.copy(osc1Wave = it)) },
        range = patch.osc1Range,
        ranges = OscRange.standard,
        onRange = { onPatch(patch.copy(osc1Range = it)) },
        level = patch.osc1Level,
        onLevel = { onPatch(patch.copy(osc1Level = it)) },
        tune = null,
        onTune = {},
        tuneLabel = "TUNE",
        tuneText = "",
        modifier = modifier
    )
}

@Composable
private fun OscillatorCardTwo(patch: Patch, onPatch: (Patch) -> Unit, modifier: Modifier) {
    OscillatorCard(
        index = 2,
        wave = patch.osc2Wave,
        onWave = { onPatch(patch.copy(osc2Wave = it)) },
        range = patch.osc2Range,
        ranges = OscRange.standard,
        onRange = { onPatch(patch.copy(osc2Range = it)) },
        level = patch.osc2Level,
        onLevel = { onPatch(patch.copy(osc2Level = it)) },
        tune = patch.osc2Tune,
        onTune = { onPatch(patch.copy(osc2Tune = it)) },
        tuneLabel = "FREQ",
        tuneText = Fmt.oscTune(patch.osc2Tune),
        modifier = modifier
    )
}

@Composable
private fun OscillatorCardThree(patch: Patch, onPatch: (Patch) -> Unit, modifier: Modifier) {
    val isLow = patch.osc3Range == OscRange.LO
    OscillatorCard(
        index = 3,
        wave = patch.osc3Wave,
        onWave = { onPatch(patch.copy(osc3Wave = it)) },
        range = patch.osc3Range,
        ranges = OscRange.withLow,
        onRange = { onPatch(patch.copy(osc3Range = it)) },
        level = patch.osc3Level,
        onLevel = { onPatch(patch.copy(osc3Level = it)) },
        tune = patch.osc3Tune,
        onTune = { onPatch(patch.copy(osc3Tune = it)) },
        tuneLabel = if (isLow) "RATE" else "FREQ",
        tuneText = if (isLow) "${Fmt.hz(Maps.osc3RateHz(patch.osc3Tune))} Hz" else Fmt.oscTune(patch.osc3Tune),
        footer = {
            LabeledToggle(
                label = "OSC 3 → FILTER",
                checked = patch.osc3ToFilter,
                onCheckedChange = { onPatch(patch.copy(osc3ToFilter = it)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier
    )
}

@Composable
private fun OscillatorCard(
    index: Int,
    wave: OscWave,
    onWave: (OscWave) -> Unit,
    range: OscRange,
    ranges: List<OscRange>,
    onRange: (OscRange) -> Unit,
    level: Float,
    onLevel: (Float) -> Unit,
    tune: Float?,
    onTune: (Float) -> Unit,
    tuneLabel: String,
    tuneText: String,
    footer: (@Composable () -> Unit)? = null,
    modifier: Modifier
) {
    val metrics = LocalPanelMetrics.current
    SectionPlate(
        title = "Oscillator $index",
        subtitle = "VCO $index",
        modifier = modifier
    ) {
        WaveformSelector(selected = wave, onSelect = onWave)
        Spacer(Modifier.height(4.dp))
        SegmentedSwitch(
            options = ranges.map { it.label },
            selectedIndex = ranges.indexOf(range).coerceAtLeast(0),
            onSelect = { onRange(ranges[it]) }
        )
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (tune != null) {
                SynthKnob(
                    label = tuneLabel,
                    value = tune,
                    onValueChange = onTune,
                    modifier = Modifier.weight(1f),
                    diameter = metrics.knob,
                    defaultValue = 0f,
                    valueText = tuneText
                )
            }
            SynthKnob(
                label = "LEVEL",
                value = level,
                onValueChange = onLevel,
                modifier = Modifier.weight(1f),
                diameter = metrics.knob,
                defaultValue = 0.5f,
                valueText = Fmt.percent(level)
            )
        }
        if (footer != null) {
            Spacer(Modifier.height(4.dp))
            footer()
        }
    }
}

// ── Mixer ─────────────────────────────────────────────────────────────────────

@Composable
private fun MixerPage(
    patch: Patch,
    onPatch: (Patch) -> Unit,
    soundActive: Boolean,
    copyScope: (FloatArray) -> Unit
) {
    val metrics = LocalPanelMetrics.current
    SectionPlate(title = "Mixer", subtitle = "OSC 1 · 2 · 3 · NOISE · DRIVE") {
        Row(Modifier.fillMaxWidth()) {
            SynthKnob("OSC 1", patch.osc1Level, { onPatch(patch.copy(osc1Level = it)) }, Modifier.weight(1f), metrics.knob, 0.78f, Fmt.percent(patch.osc1Level))
            SynthKnob("OSC 2", patch.osc2Level, { onPatch(patch.copy(osc2Level = it)) }, Modifier.weight(1f), metrics.knob, 0.55f, Fmt.percent(patch.osc2Level))
            SynthKnob("OSC 3", patch.osc3Level, { onPatch(patch.copy(osc3Level = it)) }, Modifier.weight(1f), metrics.knob, 0f, Fmt.percent(patch.osc3Level))
            SynthKnob("NOISE", patch.noiseLevel, { onPatch(patch.copy(noiseLevel = it)) }, Modifier.weight(1f), metrics.knob, 0f, Fmt.percent(patch.noiseLevel))
            SynthKnob("DRIVE", patch.drive, { onPatch(patch.copy(drive = it)) }, Modifier.weight(1f), metrics.knob, 0.15f, Fmt.percent(patch.drive))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "PUSH THE MIXER FOR ANALOG GRIT · THE LADDER SATURATES",
            color = CreamFaint,
            fontFamily = PanelFont,
            fontSize = 6.5.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionPlate(title = "Output", subtitle = "VCA", modifier = Modifier.weight(1.15f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SynthKnob(
                    label = "MASTER",
                    value = patch.masterVolume,
                    onValueChange = { onPatch(patch.copy(masterVolume = it)) },
                    modifier = Modifier.weight(1f),
                    diameter = metrics.knob,
                    defaultValue = 0.78f,
                    valueText = Fmt.percent(patch.masterVolume)
                )
                Column(Modifier.weight(1.25f), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutputMeter(
                        active = soundActive,
                        copyScope = copyScope,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (metrics.compact) 30.dp else 38.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (soundActive) "SIGNAL" else "IDLE",
                        color = if (soundActive) Amber else CreamFaint,
                        fontFamily = PanelFont,
                        fontSize = 6.5.sp,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
        SectionPlate(title = "Signal", subtitle = "MONO", modifier = Modifier.weight(0.85f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedIndicator(soundActive, Modifier.size(5.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "LAST NOTE",
                    color = CreamDim,
                    fontFamily = PanelFont,
                    fontSize = 7.sp,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(5.dp))
            Text("3 × VCO", color = Cream, fontFamily = PanelFont, fontSize = 7.5.sp, letterSpacing = 0.8.sp)
            Text("4-POLE LADDER", color = CreamDim, fontFamily = PanelFont, fontSize = 7.sp, letterSpacing = 0.6.sp)
            Text("32-BIT FLOAT", color = CreamFaint, fontFamily = PanelFont, fontSize = 6.5.sp, letterSpacing = 0.6.sp)
        }
    }
}

// ── Filter ────────────────────────────────────────────────────────────────────

@Composable
private fun FilterPage(patch: Patch, onPatch: (Patch) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 540.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterControls(patch, onPatch, Modifier.weight(1f).fillMaxHeight())
                FilterDisplay(patch, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDisplay(patch, Modifier.fillMaxWidth())
                FilterControls(patch, onPatch, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FilterControls(patch: Patch, onPatch: (Patch) -> Unit, modifier: Modifier) {
    val metrics = LocalPanelMetrics.current
    SectionPlate(title = "Modifiers", subtitle = "24 dB / OCT", modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            SynthKnob(
                label = "CUTOFF",
                value = patch.cutoff,
                onValueChange = { onPatch(patch.copy(cutoff = it)) },
                modifier = Modifier.weight(1.2f),
                diameter = metrics.largeKnob,
                defaultValue = 0.55f,
                valueText = "${Fmt.hz(Maps.cutoffHz(patch.cutoff))} Hz"
            )
            SynthKnob(
                label = "EMPHASIS",
                value = patch.emphasis,
                onValueChange = { onPatch(patch.copy(emphasis = it)) },
                modifier = Modifier.weight(1f),
                diameter = metrics.knob,
                defaultValue = 0.2f,
                valueText = Fmt.percent(patch.emphasis)
            )
            SynthKnob(
                label = "CONTOUR",
                value = patch.contourAmount,
                onValueChange = { onPatch(patch.copy(contourAmount = it)) },
                modifier = Modifier.weight(1f),
                diameter = metrics.knob,
                defaultValue = 0.45f,
                valueText = Fmt.percent(patch.contourAmount)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = "KEYBOARD TRACKING",
            color = CreamFaint,
            fontFamily = PanelFont,
            fontSize = 6.5.sp,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(3.dp))
        SegmentedSwitch(
            options = KeyTrack.entries.map { it.label },
            selectedIndex = KeyTrack.entries.indexOf(patch.keyTrack).coerceAtLeast(0),
            onSelect = { onPatch(patch.copy(keyTrack = KeyTrack.entries[it])) }
        )
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LabeledToggle(
                label = "CONTOUR DECAY",
                checked = patch.filterDecayOn,
                onCheckedChange = { onPatch(patch.copy(filterDecayOn = it)) }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (patch.filterDecayOn) "ADS" else "HOLD",
                color = if (patch.filterDecayOn) Amber else CreamFaint,
                fontFamily = PanelFont,
                fontSize = 7.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun FilterDisplay(patch: Patch, modifier: Modifier) {
    val metrics = LocalPanelMetrics.current
    SectionPlate(
        title = "Response",
        subtitle = "${Fmt.percent(patch.emphasis)} RES",
        modifier = modifier
    ) {
        FilterResponseGraph(
            cutoffHz = Maps.cutoffHz(patch.cutoff),
            resonance = patch.emphasis,
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.graphHeight)
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("20 Hz", color = CreamFaint, fontFamily = PanelFont, fontSize = 6.sp)
            Text(
                "F0 ${Fmt.hz(Maps.cutoffHz(patch.cutoff))} Hz",
                color = Amber,
                fontFamily = PanelFont,
                fontSize = 6.5.sp,
                letterSpacing = 0.6.sp
            )
            Text("20 kHz", color = CreamFaint, fontFamily = PanelFont, fontSize = 6.sp)
        }
    }
}

// ── Envelopes ─────────────────────────────────────────────────────────────────

@Composable
private fun EnvelopePage(patch: Patch, onPatch: (Patch) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 540.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterEnvelopePlate(patch, onPatch, Modifier.weight(1f).fillMaxHeight())
                LoudnessEnvelopePlate(patch, onPatch, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterEnvelopePlate(patch, onPatch, Modifier.fillMaxWidth())
                LoudnessEnvelopePlate(patch, onPatch, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FilterEnvelopePlate(patch: Patch, onPatch: (Patch) -> Unit, modifier: Modifier) {
    val metrics = LocalPanelMetrics.current
    val filterRelease = Maps.filterReleaseSeconds(patch.filterDecay)
    SectionPlate(title = "Filter Contour", subtitle = "ADS", modifier = modifier) {
        EnvelopeGraph(
            attack = patch.filterAttack,
            decay = patch.filterDecay,
            sustain = patch.filterSustain,
            release = patch.filterDecay,
            decayEnabled = patch.filterDecayOn,
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.graphHeight)
        )
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            SynthKnob(
                "ATTACK",
                patch.filterAttack,
                { onPatch(patch.copy(filterAttack = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0f,
                Fmt.pitchSeconds(Maps.filterAttackSeconds(patch.filterAttack))
            )
            SynthKnob(
                "DECAY",
                patch.filterDecay,
                { onPatch(patch.copy(filterDecay = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0.45f,
                Fmt.pitchSeconds(Maps.filterDecaySeconds(patch.filterDecay))
            )
            SynthKnob(
                "SUSTAIN",
                patch.filterSustain,
                { onPatch(patch.copy(filterSustain = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0.2f,
                Fmt.percent(patch.filterSustain)
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LabeledToggle(
                label = "DECAY",
                checked = patch.filterDecayOn,
                onCheckedChange = { onPatch(patch.copy(filterDecayOn = it)) }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "REL ${Fmt.pitchSeconds(filterRelease)}",
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 6.5.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun LoudnessEnvelopePlate(patch: Patch, onPatch: (Patch) -> Unit, modifier: Modifier) {
    val metrics = LocalPanelMetrics.current
    SectionPlate(title = "Loudness Contour", subtitle = "ADSR", modifier = modifier) {
        EnvelopeGraph(
            attack = patch.ampAttack,
            decay = patch.ampDecay,
            sustain = patch.ampSustain,
            release = patch.ampRelease,
            decayEnabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.graphHeight)
        )
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            SynthKnob(
                "ATTACK",
                patch.ampAttack,
                { onPatch(patch.copy(ampAttack = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0f,
                Fmt.pitchSeconds(Maps.ampAttackSeconds(patch.ampAttack))
            )
            SynthKnob(
                "DECAY",
                patch.ampDecay,
                { onPatch(patch.copy(ampDecay = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0.4f,
                Fmt.pitchSeconds(Maps.ampDecaySeconds(patch.ampDecay))
            )
            SynthKnob(
                "SUSTAIN",
                patch.ampSustain,
                { onPatch(patch.copy(ampSustain = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0.75f,
                Fmt.percent(patch.ampSustain)
            )
            SynthKnob(
                "RELEASE",
                patch.ampRelease,
                { onPatch(patch.copy(ampRelease = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0.18f,
                Fmt.pitchSeconds(Maps.ampReleaseSeconds(patch.ampRelease))
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "KEY RELEASE",
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 6.5.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "MONO",
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 6.5.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Performance ───────────────────────────────────────────────────────────────

@Composable
private fun PerformancePage(
    patch: Patch,
    onPatch: (Patch) -> Unit,
    onRandomize: () -> Unit,
    onPanic: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 540.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModulationPlate(patch, onPatch, Modifier.weight(1f).fillMaxHeight())
                InstrumentPlate(patch, onPatch, onRandomize, onPanic, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModulationPlate(patch, onPatch, Modifier.fillMaxWidth())
                InstrumentPlate(patch, onPatch, onRandomize, onPanic, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ModulationPlate(patch: Patch, onPatch: (Patch) -> Unit, modifier: Modifier) {
    val metrics = LocalPanelMetrics.current
    SectionPlate(title = "Modulation", subtitle = "CONTROLLERS", modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            SynthKnob(
                "GLIDE",
                patch.glide,
                { onPatch(patch.copy(glide = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0.06f,
                Fmt.glide(patch.glide)
            )
            SynthKnob(
                "MOD MIX",
                patch.modMix,
                { onPatch(patch.copy(modMix = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0f,
                "OSC ${100 - (patch.modMix * 100).toInt()}"
            )
            SynthKnob(
                "DRIFT",
                patch.drift,
                { onPatch(patch.copy(drift = it)) },
                Modifier.weight(1f),
                metrics.knob,
                0.35f,
                Fmt.percent(patch.drift)
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("OSC 3", color = CreamFaint, fontFamily = PanelFont, fontSize = 6.sp)
            Text("NOISE", color = CreamFaint, fontFamily = PanelFont, fontSize = 6.sp)
        }
        Spacer(Modifier.height(4.dp))
        LabeledToggle(
            label = "OSC 3 → FILTER MOD",
            checked = patch.osc3ToFilter,
            onCheckedChange = { onPatch(patch.copy(osc3ToFilter = it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InstrumentPlate(
    patch: Patch,
    onPatch: (Patch) -> Unit,
    onRandomize: () -> Unit,
    onPanic: () -> Unit,
    modifier: Modifier
) {
    SectionPlate(title = "Instrument", subtitle = "MONO", modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            SynthKnob(
                "TUNE",
                patch.masterTune,
                { onPatch(patch.copy(masterTune = it)) },
                Modifier.weight(1f),
                LocalPanelMetrics.current.knob,
                0f,
                Fmt.masterTune(patch.masterTune)
            )
            Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                InfoLine("SINGLE VOICE", Cream)
                InfoLine("LAST-NOTE PRIORITY", CreamDim)
                InfoLine("3 VCO / LADDER FILTER", CreamDim)
                InfoLine("NO SAMPLES · NO NETWORK", Amber)
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EngravedButton("RANDOM PATCH", onRandomize, Modifier.weight(1.2f), accent = true)
            EngravedButton("PANIC", onPanic, Modifier.weight(1f))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = "TIP · DOUBLE-TAP A KNOB TO RESET. DRAG VERTICALLY FOR FINE CONTROL.",
            color = CreamFaint,
            fontFamily = PanelFont,
            fontSize = 6.5.sp,
            letterSpacing = 0.7.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun InfoLine(text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(3.dp)
                .background(color.copy(alpha = 0.7f), CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            color = color,
            fontFamily = PanelFont,
            fontSize = 7.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
