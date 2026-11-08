package com.monarchd.synth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.monarchd.synth.R

// ── Instrument palette ────────────────────────────────────────────────────────
val Ink = Color(0xFF070606)
val InkSoft = Color(0xFF0D0B0A)
val Panel = Color(0xFF16151A)
val PanelHigh = Color(0xFF1E1D23)
val PanelEdge = Color(0xFF35333C)
val PanelInset = Color(0xFF101015)

val Cream = Color(0xFFEDE3CF)
val CreamDim = Color(0xFFB5A993)
val CreamFaint = Color(0xFF8E8474)

val Amber = Color(0xFFE5A54D)
val AmberBright = Color(0xFFFFCB7C)
val AmberDeep = Color(0xFF8E6127)
val AmberGlow = Color(0x33FFB454)

val SignalRed = Color(0xFFD25B44)
val Oxblood = Color(0xFF6B2A2A)

val Wood = Color(0xFF3A2113)
val WoodDark = Color(0xFF190D07)
val WoodLight = Color(0xFF5E3821)

val Steel = Color(0xFF92929B)
val SteelDark = Color(0xFF4B4A52)

val KeyWhite = Color(0xFFF2EAD8)
val KeyWhiteShade = Color(0xFFC9BCA2)
val KeyBlack = Color(0xFF111015)
val KeyBlackHi = Color(0xFF3D3A45)
val LedOff = Color(0xFF3A3228)

val PanelFont = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold)
)
val DisplayFont = FontFamily.Serif
val ReadoutFont = FontFamily.Monospace

private val SynthTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        letterSpacing = 4.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PanelFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 3.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PanelFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 1.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PanelFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PanelFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PanelFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PanelFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PanelFont,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 1.sp
    )
)

private val MonarchColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF241505),
    primaryContainer = AmberDeep,
    onPrimaryContainer = Cream,
    secondary = Cream,
    onSecondary = Ink,
    tertiary = SignalRed,
    background = Ink,
    onBackground = Cream,
    surface = Panel,
    onSurface = Cream,
    surfaceVariant = PanelHigh,
    onSurfaceVariant = CreamDim,
    outline = PanelEdge,
    outlineVariant = SteelDark,
    error = SignalRed,
    onError = Ink
)

@Composable
fun MonarchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonarchColors,
        typography = SynthTypography,
        content = content
    )
}
