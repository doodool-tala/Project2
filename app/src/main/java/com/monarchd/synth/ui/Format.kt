package com.monarchd.synth.ui

import com.monarchd.synth.model.Maps
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {

    fun percent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%"

    fun hz(value: Float): String = when {
        value >= 10_000f -> String.format(Locale.US, "%.1f k", value / 1000f)
        value >= 1_000f -> String.format(Locale.US, "%.2f k", value / 1000f)
        value >= 100f -> "${value.roundToInt()}"
        else -> String.format(Locale.US, "%.0f", value)
    }

    fun seconds(value: Float): String = when {
        value < 0.0095f -> String.format(Locale.US, "%.1f ms", value * 1000f)
        value < 1f -> "${(value * 1000).roundToInt()} ms"
        value < 10f -> String.format(Locale.US, "%.2f s", value)
        else -> "${value.roundToInt()} s"
    }

    fun pitchSeconds(value: Float): String = when {
        value < 0.0095f -> String.format(Locale.US, "%.1f ms", value * 1000f)
        value < 1f -> "${(value * 1000).roundToInt()} ms"
        else -> String.format(Locale.US, "%.1f s", value)
    }

    /** Normalized -1..1 oscillator tune knob shown as semitones. */
    fun oscTune(value: Float): String {
        val semitones = Maps.oscTuneSemitones(value)
        if (abs(semitones) < 0.05f) return "0 st"
        return String.format(Locale.US, "%+.1f st", semitones)
    }

    fun masterTune(value: Float): String {
        val semitones = Maps.masterTuneSemitones(value)
        if (abs(semitones) < 0.05f) return "0 ct"
        return String.format(Locale.US, "%+.0f ct", semitones * 100f)
    }

    fun glide(value: Float): String {
        val seconds = Maps.glideSeconds(value)
        return if (seconds < 0.005f) "OFF" else pitchSeconds(seconds)
    }

    fun category(value: String): String = value.uppercase(Locale.US)

    fun noteName(midi: Int): String {
        val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val pitch = ((midi % 12) + 12) % 12
        return "${names[pitch]}${midi / 12 - 1}"
    }
}
