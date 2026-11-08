package com.monarchd.synth.audio

import com.monarchd.synth.model.KeyTrack
import com.monarchd.synth.model.OscRange
import com.monarchd.synth.model.OscWave
import com.monarchd.synth.model.Patch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal const val TEST_SAMPLE_RATE = 48_000f

internal fun testPatch(
    wave: OscWave = OscWave.TRIANGLE,
    cutoff: Float = 1f,
    emphasis: Float = 0f,
    ampAttack: Float = 0.005f,
    ampDecay: Float = 0.5f,
    ampSustain: Float = 1f,
    ampRelease: Float = 0.05f,
    glide: Float = 0f,
    drift: Float = 0f,
    drive: Float = 0f,
    masterVolume: Float = 0.5f,
    osc1Level: Float = 0.9f,
    osc2Level: Float = 0f,
    osc3Level: Float = 0f,
    noiseLevel: Float = 0f,
    osc1Range: OscRange = OscRange.EIGHT,
    osc2Range: OscRange = OscRange.EIGHT,
    osc3Range: OscRange = OscRange.EIGHT,
    osc2Tune: Float = 0f,
    osc3Tune: Float = 0f,
    osc3ToFilter: Boolean = false,
    contourAmount: Float = 0f,
    keyTrack: KeyTrack = KeyTrack.OFF,
    filterAttack: Float = 0f,
    filterDecay: Float = 0.5f,
    filterSustain: Float = 1f,
    filterDecayOn: Boolean = false
): Patch = Patch(
    osc1Range = osc1Range,
    osc1Wave = wave,
    osc1Level = osc1Level,
    osc2Range = osc2Range,
    osc2Wave = wave,
    osc2Level = osc2Level,
    osc2Tune = osc2Tune,
    osc3Range = osc3Range,
    osc3Wave = OscWave.TRIANGLE,
    osc3Level = osc3Level,
    osc3Tune = osc3Tune,
    osc3ToFilter = osc3ToFilter,
    noiseLevel = noiseLevel,
    drive = drive,
    cutoff = cutoff,
    emphasis = emphasis,
    contourAmount = contourAmount,
    keyTrack = keyTrack,
    filterAttack = filterAttack,
    filterDecay = filterDecay,
    filterSustain = filterSustain,
    filterDecayOn = filterDecayOn,
    ampAttack = ampAttack,
    ampDecay = ampDecay,
    ampSustain = ampSustain,
    ampRelease = ampRelease,
    glide = glide,
    modMix = 0f,
    masterTune = 0f,
    masterVolume = masterVolume,
    drift = drift
)

/** Offline driver for the exact [SynthVoice] used by the AudioTrack loop. */
internal class Rig(val sampleRate: Float = TEST_SAMPLE_RATE) {
    val voice = SynthVoice(sampleRate)
    private var patch: Patch = testPatch()

    init {
        voice.setPatch(patch)
    }

    fun use(newPatch: Patch) {
        patch = newPatch
        voice.setPatch(newPatch)
    }

    fun noteOn(note: Int) = voice.noteOn(note)
    fun noteOff(note: Int) = voice.noteOff(note)
    fun allNotesOff() = voice.allNotesOff()
    fun wheels(mod: Float, bend: Float) = voice.setWheels(mod, bend)

    fun render(frames: Int): FloatArray {
        val out = FloatArray(frames)
        var i = 0
        while (i < frames) {
            out[i] = voice.render()
            i++
        }
        return out
    }

    fun render(seconds: Float): FloatArray = render((seconds * sampleRate).roundToInt())

    /** Update controls at block boundaries, as the real audio loop does. */
    fun renderAutomated(
        seconds: Float,
        blockFrames: Int = 64,
        control: (timeSeconds: Float) -> Unit
    ): FloatArray {
        val total = (seconds * sampleRate).roundToInt()
        val out = FloatArray(total)
        var offset = 0
        while (offset < total) {
            control(offset / sampleRate)
            val count = min(blockFrames, total - offset)
            var i = 0
            while (i < count) {
                out[offset + i] = voice.render()
                i++
            }
            offset += count
        }
        return out
    }
}

internal fun concat(vararg parts: FloatArray): FloatArray {
    val result = FloatArray(parts.sumOf { it.size })
    var offset = 0
    for (part in parts) {
        part.copyInto(result, offset)
        offset += part.size
    }
    return result
}

internal fun peak(samples: FloatArray): Float = samples.maxOfOrNull { abs(it) } ?: 0f

internal fun rms(samples: FloatArray, from: Int = 0, to: Int = samples.size): Double {
    if (to <= from) return 0.0
    var sum = 0.0
    var i = from
    while (i < to) {
        sum += samples[i].toDouble() * samples[i]
        i++
    }
    return sqrt(sum / (to - from))
}

internal fun mean(samples: FloatArray, from: Int = 0, to: Int = samples.size): Double {
    if (to <= from) return 0.0
    var sum = 0.0
    var i = from
    while (i < to) {
        sum += samples[i]
        i++
    }
    return sum / (to - from)
}

internal fun allFinite(samples: FloatArray): Boolean = samples.all { it.isFinite() }

internal fun maxStep(samples: FloatArray, from: Int, to: Int): Float {
    var maxDelta = 0f
    var i = max(1, from)
    val end = min(to, samples.size)
    while (i < end) {
        maxDelta = max(maxDelta, abs(samples[i] - samples[i - 1]))
        i++
    }
    return maxDelta
}

internal fun rmsEnvelope(samples: FloatArray, sampleRate: Float, windowSeconds: Float = 0.005f): FloatArray {
    val window = max(1, (windowSeconds * sampleRate).roundToInt())
    val frames = samples.size / window
    val result = FloatArray(frames)
    var frame = 0
    while (frame < frames) {
        val start = frame * window
        result[frame] = rms(samples, start, start + window).toFloat()
        frame++
    }
    return result
}

internal data class EnvelopeMeasurement(
    val attackSeconds: Double,
    val peakSeconds: Double,
    val decaySeconds: Double,
    val sustain: Double,
    val releaseSeconds: Double,
    val peak: Float
)

/**
 * Measures the loudness contour of a note-on buffer followed by a note-off
 * buffer. Times use 5 ms RMS windows: attack is 90% of peak, decay is 95% of
 * the way to sustain, release is -26 dB from peak.
 */
internal fun measureEnvelope(
    noteOn: FloatArray,
    noteOff: FloatArray,
    sampleRate: Float = TEST_SAMPLE_RATE
): EnvelopeMeasurement {
    val window = 0.005f
    val whole = concat(noteOn, noteOff)
    val env = rmsEnvelope(whole, sampleRate, window)
    if (env.isEmpty()) return EnvelopeMeasurement(0.0, 0.0, 0.0, 0.0, 0.0, 0f)
    val peakEnv = env.maxOrNull() ?: 0f
    val peakIndex = env.indexOfFirst { it == peakEnv }.coerceAtLeast(0)
    val noteOffFrame = noteOn.size / max(1, (window * sampleRate).roundToInt())

    var attackFrame = 0
    while (attackFrame < env.size && env[attackFrame] < peakEnv * 0.9f) attackFrame++

    val sustainStart = max(peakIndex, noteOffFrame - 40)
    var sustainSum = 0.0
    var count = 0
    var i = sustainStart
    while (i < noteOffFrame) {
        sustainSum += env[i]
        count++
        i++
    }
    val sustain = if (count > 0) sustainSum / count / peakEnv else 0.0

    var decayFrame = peakIndex
    val decayTarget = sustain + (1.0 - sustain) * 0.05
    while (decayFrame < noteOffFrame && env[decayFrame] > decayTarget * peakEnv) decayFrame++

    var releaseFrame = noteOffFrame
    val releaseTarget = (sustain * peakEnv * 0.05f).toFloat().coerceAtLeast(0.0002f)
    while (releaseFrame < env.size && env[releaseFrame] > releaseTarget) releaseFrame++

    return EnvelopeMeasurement(
        attackSeconds = attackFrame * window.toDouble(),
        peakSeconds = peakIndex * window.toDouble(),
        decaySeconds = max(0.0, (decayFrame - peakIndex) * window.toDouble()),
        sustain = sustain,
        releaseSeconds = max(0.0, (releaseFrame - noteOffFrame) * window.toDouble()),
        peak = peakEnv
    )
}

internal fun cents(measured: Double, expected: Double): Double = 1200.0 * log2(measured / expected)

/**
 * YIN fundamental estimator. Accurate enough for a cents-level tuning check and
 * robust to the strong harmonics of the saw and pulse waves.
 */
internal fun estimatePitch(
    samples: FloatArray,
    sampleRate: Float = TEST_SAMPLE_RATE,
    minHz: Float = 25f,
    maxHz: Float = 4_200f
): Double {
    if (samples.size < 64) return 0.0
    val values = samples.copyOf()
    val average = mean(values)
    for (i in values.indices) values[i] = (values[i] - average).toFloat()

    val maxTau = min((sampleRate / minHz).toInt(), values.size / 2)
    val minTau = max(2, (sampleRate / maxHz).toInt())
    if (maxTau <= minTau + 2) return 0.0
    val window = values.size - maxTau
    val difference = DoubleArray(maxTau + 1)
    var tau = 1
    while (tau <= maxTau) {
        var sum = 0.0
        var i = 0
        while (i < window) {
            val delta = values[i] - values[i + tau]
            sum += delta * delta
            i++
        }
        difference[tau] = sum
        tau++
    }

    val normalized = DoubleArray(maxTau + 1)
    normalized[0] = 1.0
    var running = 0.0
    tau = 1
    while (tau <= maxTau) {
        running += difference[tau]
        normalized[tau] = if (running <= 0.0) 1.0 else difference[tau] * tau / running
        tau++
    }

    var bestTau = -1
    tau = minTau
    while (tau < maxTau) {
        if (normalized[tau] < 0.22) {
            while (tau + 1 <= maxTau && normalized[tau + 1] < normalized[tau]) tau++
            bestTau = tau
            break
        }
        tau++
    }
    if (bestTau < 0) {
        bestTau = minTau
        tau = minTau + 1
        while (tau <= maxTau) {
            if (normalized[tau] < normalized[bestTau]) bestTau = tau
            tau++
        }
    }

    var refined = bestTau.toDouble()
    if (bestTau in 1 until maxTau) {
        val a = normalized[bestTau - 1]
        val b = normalized[bestTau]
        val c = normalized[bestTau + 1]
        val denominator = a - 2.0 * b + c
        if (abs(denominator) > 1e-12) {
            refined += 0.5 * (a - c) / denominator
        }
    }
    return if (refined > 0.0) sampleRate / refined else 0.0
}

/** Hann-windowed FFT magnitude spectrum, zero-padded to [fftSize]. */
internal fun fftMagnitudes(
    samples: FloatArray,
    sampleRate: Float = TEST_SAMPLE_RATE,
    fftSize: Int = 65_536
): DoubleArray {
    require(fftSize > 0 && fftSize and (fftSize - 1) == 0) { "fftSize must be a power of two" }
    val re = DoubleArray(fftSize)
    val im = DoubleArray(fftSize)
    val count = min(samples.size, fftSize)
    var i = 0
    while (i < count) {
        val windowValue = 0.5 - 0.5 * cos(2.0 * PI * i / (count - 1).coerceAtLeast(1))
        re[i] = samples[i] * windowValue
        i++
    }

    var j = 0
    i = 1
    while (i < fftSize) {
        var bit = fftSize shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            val tr = re[i]
            re[i] = re[j]
            re[j] = tr
            val ti = im[i]
            im[i] = im[j]
            im[j] = ti
        }
        i++
    }

    var length = 2
    while (length <= fftSize) {
        val angle = -2.0 * PI / length
        val wLengthRe = cos(angle)
        val wLengthIm = sin(angle)
        var start = 0
        while (start < fftSize) {
            var wRe = 1.0
            var wIm = 0.0
            var k = 0
            while (k < length / 2) {
                val even = start + k
                val odd = even + length / 2
                val oddRe = re[odd] * wRe - im[odd] * wIm
                val oddIm = re[odd] * wIm + im[odd] * wRe
                re[odd] = re[even] - oddRe
                im[odd] = im[even] - oddIm
                re[even] += oddRe
                im[even] += oddIm
                val nextRe = wRe * wLengthRe - wIm * wLengthIm
                wIm = wRe * wLengthIm + wIm * wLengthRe
                wRe = nextRe
                k++
            }
            start += length
        }
        length = length shl 1
    }

    val magnitude = DoubleArray(fftSize / 2 + 1)
    i = 0
    while (i < magnitude.size) {
        magnitude[i] = sqrt(re[i] * re[i] + im[i] * im[i])
        i++
    }
    return magnitude
}

internal fun spectralCentroid(
    samples: FloatArray,
    sampleRate: Float = TEST_SAMPLE_RATE
): Double {
    val fftSize = 65_536
    val magnitude = fftMagnitudes(samples, sampleRate, fftSize)
    val binHz = sampleRate.toDouble() / fftSize
    var weighted = 0.0
    var total = 0.0
    var bin = 1
    while (bin < magnitude.size) {
        val frequency = bin * binHz
        if (frequency in 30.0..min(12_000.0, sampleRate / 2.0)) {
            weighted += frequency * magnitude[bin]
            total += magnitude[bin]
        }
        bin++
    }
    return if (total > 0.0) weighted / total else 0.0
}

internal fun bandEnergy(
    samples: FloatArray,
    lowHz: Double,
    highHz: Double,
    sampleRate: Float = TEST_SAMPLE_RATE
): Double {
    val fftSize = 65_536
    val magnitude = fftMagnitudes(samples, sampleRate, fftSize)
    val binHz = sampleRate.toDouble() / fftSize
    var energy = 0.0
    var bin = 1
    while (bin < magnitude.size) {
        val frequency = bin * binHz
        if (frequency in lowHz..highHz) energy += magnitude[bin] * magnitude[bin]
        bin++
    }
    return energy
}

/** Hann-windowed Goertzel magnitude with amplitude correction. */
internal fun toneMagnitude(
    samples: FloatArray,
    frequency: Double,
    sampleRate: Float = TEST_SAMPLE_RATE
): Double {
    if (samples.size < 8) return 0.0
    val coefficient = 2.0 * cos(2.0 * PI * frequency / sampleRate)
    var s1 = 0.0
    var s2 = 0.0
    var windowSum = 0.0
    var i = 0
    while (i < samples.size) {
        val windowValue = 0.5 - 0.5 * cos(2.0 * PI * i / (samples.size - 1))
        val s0 = samples[i] * windowValue + coefficient * s1 - s2
        s2 = s1
        s1 = s0
        windowSum += windowValue
        i++
    }
    val power = s1 * s1 + s2 * s2 - coefficient * s1 * s2
    return if (power > 0.0 && windowSum > 0.0) 2.0 * sqrt(power) / windowSum else 0.0
}

internal fun harmonicRatios(
    samples: FloatArray,
    fundamentalHz: Double,
    count: Int = 8,
    sampleRate: Float = TEST_SAMPLE_RATE
): DoubleArray {
    val fundamental = toneMagnitude(samples, fundamentalHz, sampleRate)
    return DoubleArray(count) { index ->
        val harmonic = toneMagnitude(samples, fundamentalHz * (index + 2), sampleRate)
        if (fundamental > 0.0) harmonic / fundamental else 0.0
    }
}

internal fun writeMonoWav(
    file: File,
    samples: FloatArray,
    sampleRate: Int = TEST_SAMPLE_RATE.toInt(),
    normalizeTo: Float = 0.89f
) {
    val rawPeak = peak(samples)
    val gain = if (rawPeak > 1e-6f) normalizeTo / rawPeak else 0f
    val dataBytes = samples.size * 2
    val header = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(36 + dataBytes)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1)
    header.putShort(1)
    header.putInt(sampleRate)
    header.putInt(sampleRate * 2)
    header.putShort(2)
    header.putShort(16)
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(dataBytes)
    for (sample in samples) {
        val value = (sample * gain * 32_767f).roundToInt().coerceIn(-32_768, 32_767)
        header.putShort(value.toShort())
    }
    file.parentFile?.mkdirs()
    file.writeBytes(header.array())
}

internal fun db(ratio: Double): Double = if (ratio > 0.0) 20.0 * ln(ratio) / ln(10.0) else -120.0
