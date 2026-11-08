package com.monarchd.synth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monarchd.synth.model.AppSettings
import com.monarchd.synth.ui.components.EngravedButton
import com.monarchd.synth.ui.components.LogoMark
import com.monarchd.synth.ui.theme.Amber
import com.monarchd.synth.ui.theme.AmberBright
import com.monarchd.synth.ui.theme.AmberDeep
import com.monarchd.synth.ui.theme.Cream
import com.monarchd.synth.ui.theme.CreamDim
import com.monarchd.synth.ui.theme.CreamFaint
import com.monarchd.synth.ui.theme.DisplayFont
import com.monarchd.synth.ui.theme.Ink
import com.monarchd.synth.ui.theme.Panel
import com.monarchd.synth.ui.theme.PanelEdge
import com.monarchd.synth.ui.theme.PanelFont
import com.monarchd.synth.ui.theme.PanelHigh
import com.monarchd.synth.ui.theme.PanelInset
import com.monarchd.synth.ui.theme.ReadoutFont
import com.monarchd.synth.ui.theme.SteelDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    audioSummary: String,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onPanic: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Panel,
        contentColor = Cream,
        scrimColor = Color.Black.copy(alpha = 0.62f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SheetHeader(
                title = "SETTINGS",
                subtitle = "MONARCH D · VERSION 1.0.0",
                onDismiss = onDismiss
            )
            Spacer(Modifier.height(14.dp))

            SectionCaption("INTERFACE")
            SettingToggleRow(
                title = "HAPTIC FEEDBACK",
                description = "A subtle physical tick when you turn a control or press a key.",
                checked = settings.haptics,
                onCheckedChange = { value -> onChange { it.copy(haptics = value) } }
            )
            SettingToggleRow(
                title = "KEY NAMES",
                description = "Print note names on the C keys of the keyboard.",
                checked = settings.showKeyLabels,
                onCheckedChange = { value -> onChange { it.copy(showKeyLabels = value) } }
            )

            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(10.dp))
            SectionCaption("AUDIO ENGINE")
            Text(
                text = audioSummary,
                color = Amber,
                fontFamily = ReadoutFont,
                fontSize = 9.sp,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Real-time synthesis at the device's native sample rate. Three band-limited oscillators feed a 2x oversampled nonlinear transistor ladder, followed by two analog-modeled contours. No samples and no network.",
                color = CreamDim,
                fontFamily = PanelFont,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            EngravedButton(
                text = "PANIC · RELEASE ALL NOTES",
                onClick = onPanic,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            SectionCaption("ABOUT")
            Text(
                text = "MONARCH D",
                color = Cream,
                fontFamily = DisplayFont,
                fontSize = 24.sp,
                letterSpacing = 4.sp
            )
            Text(
                text = "A modern monophonic instrument inspired by the golden age of American analog synthesis: three oscillators, a 24 dB/oct transistor ladder, and two envelope contours with a very definite personality.",
                color = CreamDim,
                fontFamily = PanelFont,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Every note is synthesised on your device. No samples, no accounts, no analytics, no network permissions.",
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Barlow Condensed · SIL Open Font License 1.1",
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 7.5.sp,
                letterSpacing = 0.4.sp
            )
            Text(
                text = "© 2026 Monarch Instruments",
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 7.5.sp,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Panel,
        contentColor = Cream,
        scrimColor = Color.Black.copy(alpha = 0.62f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SheetHeader(
                title = "HOW TO PLAY",
                subtitle = "QUICK START",
                onDismiss = onDismiss
            )
            Spacer(Modifier.height(12.dp))
            HelpRow(
                number = "01",
                title = "SHAPE THE SOUND",
                body = "Drag any knob vertically, or sideways for fine adjustment. Double-tap a knob to return it to its factory position."
            )
            HelpRow(
                number = "02",
                title = "MOVE AROUND THE PANEL",
                body = "Swipe the panel sideways or tap the engraved OSC / MIX / FILTER / ENV / PERF tabs. Five sections hold the entire instrument."
            )
            HelpRow(
                number = "03",
                title = "PLAY",
                body = "Slide across the keyboard for a glissando. The instrument is monophonic with last-note priority, just like the originals. Use the octave arrows to travel further."
            )
            HelpRow(
                number = "04",
                title = "BEND AND BREATHE",
                body = "The left wheel bends pitch and springs back. The right wheel adds vibrato and filter movement; it stays where you leave it."
            )
            HelpRow(
                number = "05",
                title = "KEEP YOUR SOUNDS",
                body = "Tap the preset name to browse the library, then use SAVE CURRENT to keep a patch of your own. SURPRISE ME generates a new musical starting point."
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Amber.copy(alpha = 0.07f), RoundedCornerShape(4.dp))
                    .border(1.dp, AmberDeep.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "TIP · Headphones or a wired speaker give the lowest latency and the deepest bass. The engine uses your device's native output rate.",
                    color = CreamDim,
                    fontFamily = PanelFont,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            EngravedButton(
                text = "GOT IT",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                accent = true
            )
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Cream,
                fontFamily = PanelFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                letterSpacing = 2.5.sp
            )
            Text(
                text = subtitle,
                color = Amber,
                fontFamily = PanelFont,
                fontSize = 7.sp,
                letterSpacing = 1.3.sp
            )
        }
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(PanelInset)
                .border(1.dp, PanelEdge.copy(alpha = 0.5f), CircleShape)
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                color = CreamDim,
                fontFamily = PanelFont,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Cream,
                fontFamily = PanelFont,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = description,
                color = CreamDim,
                fontFamily = PanelFont,
                fontSize = 10.sp,
                lineHeight = 12.5.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Amber,
                checkedBorderColor = Amber,
                uncheckedThumbColor = SteelDark,
                uncheckedTrackColor = PanelInset,
                uncheckedBorderColor = PanelEdge
            )
        )
    }
}

@Composable
private fun SectionCaption(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(2.dp).height(8.dp).background(Amber))
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            color = CreamFaint,
            fontFamily = PanelFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 7.5.sp,
            letterSpacing = 1.8.sp
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PanelEdge.copy(alpha = 0.35f))
    )
}

@Composable
private fun HelpRow(number: String, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(
            text = number,
            color = AmberDeep,
            fontFamily = ReadoutFont,
            fontSize = 9.sp,
            modifier = Modifier.width(26.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Cream,
                fontFamily = PanelFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                color = CreamDim,
                fontFamily = PanelFont,
                fontSize = 10.5.sp,
                lineHeight = 13.5.sp
            )
        }
    }
}

@Composable
fun IntroOverlay(onEnter: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2070605))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        val compact = maxHeight < 560.dp
        Box(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(PanelHigh, Panel)))
                .border(1.dp, AmberDeep.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                .padding(if (compact) 18.dp else 24.dp)
                .heightIn(max = maxHeight - 30.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LogoMark(Modifier.size(if (compact) 44.dp else 62.dp))
                Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                Text(
                    text = "MONARCH D",
                    color = Cream,
                    fontFamily = DisplayFont,
                    fontSize = if (compact) 26.sp else 32.sp,
                    letterSpacing = 5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MONOPHONIC ANALOG SYNTHESIZER",
                    color = Amber,
                    fontFamily = PanelFont,
                    fontSize = 7.sp,
                    letterSpacing = 2.4.sp,
                    textAlign = TextAlign.Center
                )
                if (!compact) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Three oscillators. One ladder. Infinite attitude.",
                        color = CreamDim,
                        fontFamily = PanelFont,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(if (compact) 10.dp else 18.dp))
                IntroTip("01", "TOUCH THE PANEL", "Drag knobs vertically. Double-tap to reset.")
                IntroTip("02", "SWIPE THE SECTIONS", "OSC, mixer, filter, envelopes and performance.")
                IntroTip("03", "PLAY", "Slide across the keys. Bend with the left wheel.")
                Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFC99040), AmberBright, Color(0xFFC99040))
                            )
                        )
                        .pointerInput(Unit) { detectTapGestures { onEnter() } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ENTER THE PANEL",
                        color = Color(0xFF1A1106),
                        fontFamily = PanelFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 2.2.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "NO SAMPLES · NO NETWORK · 100% SYNTHESIS",
                    color = CreamFaint,
                    fontFamily = PanelFont,
                    fontSize = 6.5.sp,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun IntroTip(number: String, title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            color = AmberDeep,
            fontFamily = ReadoutFont,
            fontSize = 8.sp,
            modifier = Modifier.width(24.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Cream,
                fontFamily = PanelFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.3.sp
            )
            Text(
                text = body,
                color = CreamDim,
                fontFamily = PanelFont,
                fontSize = 10.sp,
                lineHeight = 12.5.sp
            )
        }
    }
}
