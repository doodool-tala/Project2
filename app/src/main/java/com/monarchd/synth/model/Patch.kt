package com.monarchd.synth.model

import kotlin.math.pow

/**
 * Waveform shapes available on each oscillator, mirroring the six-position
 * selector of the classic instrument: triangle, triangle/sawtooth blend,
 * sawtooth, square, wide pulse and narrow pulse.
 */
enum class OscWave(val shortLabel: String, val longLabel: String) {
    TRIANGLE("TRI", "Triangle"),
    TRI_SAW("TRI/SAW", "Tri / Saw"),
    SAWTOOTH("SAW", "Sawtooth"),
    SQUARE("SQR", "Square"),
    PULSE_WIDE("PULSE W", "Wide pulse"),
    PULSE_NARROW("PULSE N", "Narrow pulse");

    val isPulse: Boolean get() = this == PULSE_WIDE || this == PULSE_NARROW

    val duty: Double
        get() = when (this) {
            PULSE_WIDE -> 0.25
            PULSE_NARROW -> 0.10
            else -> 0.5
        }
}

/** Organ-style footage switch. LO is only offered on oscillator 3. */
enum class OscRange(val label: String, val semitones: Int, val display: String) {
    LO("LO", 0, "Low freq"),
    THIRTY_TWO("32'", -24, "32 foot"),
    SIXTEEN("16'", -12, "16 foot"),
    EIGHT("8'", 0, "8 foot"),
    FOUR("4'", 12, "4 foot"),
    TWO("2'", 24, "2 foot");

    val isLow: Boolean get() = this == LO

    companion object {
        val standard: List<OscRange> = listOf(THIRTY_TWO, SIXTEEN, EIGHT, FOUR, TWO)
        val withLow: List<OscRange> = listOf(LO, THIRTY_TWO, SIXTEEN, EIGHT, FOUR, TWO)
    }
}

enum class KeyTrack(val label: String, val amount: Float) {
    OFF("OFF", 0f),
    THIRD("1/3", 0.333f),
    TWO_THIRDS("2/3", 0.667f),
    FULL("FULL", 1f)
}

enum class PresetCategory(val label: String) {
    BASS("BASS"),
    LEAD("LEAD"),
    KEYS("KEYS"),
    PAD("PAD"),
    FX("FX"),
    USER("USER")
}

/**
 * A complete panel snapshot. Every parameter is normalized to 0..1 so the UI,
 * persistence layer and audio engine all speak the same language; musical units
 * are derived in [Maps].
 */
data class Patch(
    val name: String = "Init Patch",
    val category: PresetCategory = PresetCategory.KEYS,
    val description: String = "",

    // Oscillator bank
    val osc1Range: OscRange = OscRange.EIGHT,
    val osc1Wave: OscWave = OscWave.SAWTOOTH,
    val osc1Level: Float = 0.78f,
    val osc2Range: OscRange = OscRange.EIGHT,
    val osc2Wave: OscWave = OscWave.SAWTOOTH,
    val osc2Level: Float = 0.55f,
    val osc2Tune: Float = 0.02f,
    val osc3Range: OscRange = OscRange.LO,
    val osc3Wave: OscWave = OscWave.TRIANGLE,
    val osc3Level: Float = 0f,
    val osc3Tune: Float = 0f,
    val osc3ToFilter: Boolean = false,
    val noiseLevel: Float = 0f,

    // Mixer / output stage
    val drive: Float = 0.15f,

    // Filter
    val cutoff: Float = 0.52f,
    val emphasis: Float = 0.18f,
    val contourAmount: Float = 0.45f,
    val keyTrack: KeyTrack = KeyTrack.TWO_THIRDS,
    val filterAttack: Float = 0.02f,
    val filterDecay: Float = 0.45f,
    val filterSustain: Float = 0.20f,
    val filterDecayOn: Boolean = true,

    // Loudness contour
    val ampAttack: Float = 0.01f,
    val ampDecay: Float = 0.40f,
    val ampSustain: Float = 0.75f,
    val ampRelease: Float = 0.18f,

    // Performance
    val glide: Float = 0.06f,
    val modMix: Float = 0f,
    val masterTune: Float = 0f,
    val masterVolume: Float = 0.78f,
    val drift: Float = 0.35f
) {
    val activeOscillators: Int
        get() = listOf(osc1Level, osc2Level, osc3Level).count { it > 0.01f } + if (noiseLevel > 0.01f) 1 else 0
}

/** Conversions between normalized panel values and musical units. */
object Maps {
    const val CUTOFF_MIN = 24.0
    const val CUTOFF_MAX = 16000.0

    fun cutoffHz(v: Float): Float =
        (CUTOFF_MIN * (CUTOFF_MAX / CUTOFF_MIN).pow(v.coerceIn(0f, 1f).toDouble())).toFloat()

    fun seconds(v: Float, min: Float, max: Float): Float =
        (min.toDouble() * (max.toDouble() / min.toDouble()).pow(v.coerceIn(0f, 1f).toDouble())).toFloat()

    fun oscTuneSemitones(v: Float): Float = v.coerceIn(-1f, 1f) * 7f

    /** Panel knobs use 0..1; tune parameters are centre-zero -1..1. */
    fun bipolarToUnit(v: Float): Float = ((v.coerceIn(-1f, 1f) + 1f) / 2f)

    fun unitToBipolar(v: Float): Float = v.coerceIn(0f, 1f) * 2f - 1f

    fun masterTuneSemitones(v: Float): Float = v.coerceIn(-1f, 1f) * 2f

    fun glideSeconds(v: Float): Float = v.coerceIn(0f, 1f).let { it * it * 1.6f }

    fun osc3RateHz(tune: Float): Float =
        (0.08 * (24.0 / 0.08).pow(((tune.coerceIn(-1f, 1f) + 1f) / 2f).toDouble())).toFloat()

    fun filterAttackSeconds(v: Float): Float = seconds(v, 0.001f, 8f)
    fun filterDecaySeconds(v: Float): Float = seconds(v, 0.004f, 10f)
    fun filterReleaseSeconds(v: Float): Float = seconds(v, 0.02f, 8f)
    fun ampAttackSeconds(v: Float): Float = seconds(v, 0.001f, 8f)
    fun ampDecaySeconds(v: Float): Float = seconds(v, 0.004f, 10f)
    fun ampReleaseSeconds(v: Float): Float = seconds(v, 0.02f, 8f)
}

/** A factory or user patch with display metadata. */
data class Preset(
    val name: String,
    val category: PresetCategory,
    val description: String,
    val patch: Patch,
    val isUser: Boolean = false
)
