package com.monarchd.synth.audio

import com.monarchd.synth.model.OscWave
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

internal const val SCOPE_SIZE = 256
internal const val TWO_PI = 2.0 * PI

/** Rational tanh approximation (Padé 7/6). Linear for small signals, soft at extremes. */
internal fun tanhFast(x: Float): Float {
    val c = x.coerceIn(-4f, 4f)
    val x2 = c * c
    return c * (135135f + x2 * (17325f + x2 * (378f + x2))) /
        (135135f + x2 * (62370f + x2 * (3150f + x2 * 28f)))
}

internal fun midiToHz(note: Double): Double = 440.0 * 2.0.pow((note - 69.0) / 12.0)

/** PolyBLEP correction around a waveform discontinuity. */
private fun polyBlep(t: Double, dt: Double): Double {
    if (dt <= 0.0) return 0.0
    return when {
        t < dt -> {
            val x = t / dt
            2.0 * x - x * x - 1.0
        }
        t > 1.0 - dt -> {
            val x = (t - 1.0) / dt
            x * x + 2.0 * x + 1.0
        }
        else -> 0.0
    }
}

/**
 * Band-limited oscillator. Phase increments are smoothed so control-rate pitch
 * changes (glide, vibrato, drift) never produce zipper noise.
 */
internal class Oscillator(private val sampleRate: Float) {
    var phase: Double = 0.0
        private set

    private var increment: Double = 0.0
    private var targetIncrement: Double = 0.0

    fun setFrequency(hz: Double) {
        targetIncrement = (hz / sampleRate).coerceIn(0.0, 0.48)
    }

    fun resetPhase(newPhase: Double) {
        phase = newPhase % 1.0
        increment = targetIncrement
    }

    fun advance() {
        increment += (targetIncrement - increment) * 0.004
        phase += increment
        if (phase >= 1.0) phase -= 1.0
        if (phase < 0.0) phase = 0.0
    }

    fun sample(wave: OscWave): Float {
        val t = phase
        val dt = increment
        return when (wave) {
            OscWave.TRIANGLE -> (1.0 - 4.0 * abs(t - 0.5)).toFloat()
            OscWave.TRI_SAW -> (0.5 * (1.0 - 4.0 * abs(t - 0.5)) + 0.5 * (2.0 * t - 1.0 - polyBlep(t, dt))).toFloat()
            OscWave.SAWTOOTH -> (2.0 * t - 1.0 - polyBlep(t, dt)).toFloat()
            OscWave.SQUARE -> {
                var value = if (t < 0.5) 1.0 else -1.0
                value += polyBlep(t, dt)
                value -= polyBlep((t + 0.5) % 1.0, dt)
                value.toFloat()
            }
            OscWave.PULSE_WIDE -> pulse(t, dt, 0.25)
            OscWave.PULSE_NARROW -> pulse(t, dt, 0.10)
        }
    }

    private fun pulse(t: Double, dt: Double, duty: Double): Float {
        var value = if (t < duty) 1.0 else -1.0
        value += polyBlep(t, dt)
        value -= polyBlep((t + (1.0 - duty)) % 1.0, dt)
        return value.toFloat()
    }
}

/**
 * Four-pole transistor-ladder low-pass. Each stage is a topology-preserving
 * one-pole driven through a saturating feedback path; running it at 2x the
 * sample rate keeps the resonance singing without digital harshness.
 */
internal class MoogLadder(private val sampleRate: Float) {
    private val state = FloatArray(4)
    private val oversample = 2
    private var g = 0.5f
    private var gTarget = 0.5f
    private var k = 0.0f

    var resonance: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            k = field * 4.05f
        }

    fun setCutoff(hz: Float) {
        val fc = hz.coerceIn(18f, sampleRate * 0.42f)
        val wd = TWO_PI * fc / (sampleRate * oversample)
        val t = tan(wd * 0.5)
        gTarget = (t / (1.0 + t)).toFloat().coerceIn(0.00005f, 0.9995f)
    }

    fun process(input: Float): Float {
        var output = 0f
        var pass = 0
        while (pass < oversample) {
            g += (gTarget - g) * 0.02f
            // Saturated feedback: the heart of the ladder's musical compression.
            val u = tanhFast(input - k * state[3])
            var s = u
            var stage = 0
            while (stage < 4) {
                val v = (s - state[stage]) * g
                val y = v + state[stage]
                state[stage] = y + v
                s = y
                stage++
            }
            output = s
            pass++
        }
        return output
    }

    fun reset() {
        state.fill(0f)
        g = gTarget
    }
}

/**
 * Analog-style ADS(R) envelope. Attacks charge toward an overshoot target and
 * clamp, decays release exponentially. Coefficient times are scaled so the
 * panel values correspond to musically useful durations.
 */
internal class Adsr(private val sampleRate: Float) {
    private enum class Stage { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }

    private var stage = Stage.IDLE
    private var attackCoef = 0.01f
    private var decayCoef = 0.01f
    private var releaseCoef = 0.01f
    private var sustain = 0.8f
    private var decayEnabled = true

    var value: Float = 0f
        private set

    fun setParams(
        attackSeconds: Float,
        decaySeconds: Float,
        sustainLevel: Float,
        releaseSeconds: Float,
        decayOn: Boolean
    ) {
        attackCoef = coefficient(attackSeconds)
        decayCoef = coefficient(decaySeconds)
        releaseCoef = coefficient(releaseSeconds)
        sustain = sustainLevel.coerceIn(0f, 1f)
        decayEnabled = decayOn
    }

    private fun coefficient(seconds: Float): Float {
        val tau = (seconds * 0.32f).coerceAtLeast(0.00035f)
        return (1.0 - exp(-1.0 / (tau * sampleRate))).toFloat().coerceIn(0.00001f, 1f)
    }

    val isIdle: Boolean get() = stage == Stage.IDLE

    fun retrigger() {
        stage = Stage.ATTACK
    }

    fun release() {
        if (stage != Stage.IDLE) stage = Stage.RELEASE
    }

    fun reset() {
        stage = Stage.IDLE
        value = 0f
    }

    fun process(): Float {
        when (stage) {
            Stage.ATTACK -> {
                value += (1.12f - value) * attackCoef
                if (value >= 1f) {
                    value = 1f
                    stage = if (decayEnabled) Stage.DECAY else Stage.SUSTAIN
                }
            }
            Stage.DECAY -> {
                val target = if (decayEnabled) sustain else 1f
                value += (target - value) * decayCoef
                if (abs(value - target) < 0.0008f) {
                    value = target
                    stage = Stage.SUSTAIN
                }
            }
            Stage.SUSTAIN -> value = if (decayEnabled) sustain else 1f
            Stage.RELEASE -> {
                value -= value * releaseCoef
                if (value <= 0.0002f) {
                    value = 0f
                    stage = Stage.IDLE
                }
            }
            Stage.IDLE -> value = 0f
        }
        return value
    }
}

internal class NoiseSource(seed: Int = 0x51AF3B) {
    private var state: Int = if (seed == 0) 0x51AF3B else seed

    fun next(): Float {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        state = x
        return ((x and 0xFFFF) - 0x8000) / 32768f
    }
}

/** Smooth sample-and-hold noise used as a random modulation source. */
internal class RandomModulator(sampleRate: Float) {
    private val noise = NoiseSource(0x2F19)
    private var current = 0f
    private var target = 0f
    private var countdown = 0
    private val period = (sampleRate * 0.09f / 8f).toInt().coerceAtLeast(32)

    fun next(): Float {
        if (countdown <= 0) {
            countdown = period
            target = noise.next()
        }
        countdown--
        current += (target - current) * 0.045f
        return current
    }

    fun reset() {
        current = 0f
        target = 0f
        countdown = 0
    }
}
