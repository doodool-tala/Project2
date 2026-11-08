package com.monarchd.synth.audio

import com.monarchd.synth.model.FactoryPresets
import com.monarchd.synth.model.Maps
import com.monarchd.synth.model.OscWave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

/**
 * Renders the exact voice used by the AudioTrack loop and verifies musical
 * behaviour, rather than only checking that the DSP classes compile.
 */
class AudioEngineTest {

    private fun segment(samples: FloatArray, fromSeconds: Float, toSeconds: Float): FloatArray {
        val from = (fromSeconds * TEST_SAMPLE_RATE).toInt().coerceIn(0, samples.size)
        val to = (toSeconds * TEST_SAMPLE_RATE).toInt().coerceIn(from, samples.size)
        return samples.copyOfRange(from, to)
    }

    private fun pitchOf(
        note: Int,
        seconds: Float = 1.6f,
        fromSeconds: Float = 0.45f,
        toSeconds: Float = 1.35f,
        patch: com.monarchd.synth.model.Patch = testPatch()
    ): Double {
        val rig = Rig()
        rig.use(patch)
        rig.noteOn(note)
        val audio = rig.render(seconds)
        return estimatePitch(segment(audio, fromSeconds, toSeconds), TEST_SAMPLE_RATE)
    }

    @Test
    fun tuningTracksA440AcrossTheKeyboard() {
        val notes = listOf(24, 36, 48, 60, 69, 72, 81, 84, 96)
        for (note in notes) {
            val expected = 440.0 * 2.0.pow((note - 69) / 12.0)
            val measured = pitchOf(note)
            val error = cents(measured, expected)
            assertTrue(
                "note $note expected %.3f Hz, measured %.3f Hz (%+.2f cents)".format(expected, measured, error),
                abs(error) < 3.0
            )
        }
        assertEquals("concert A", 0.0, cents(pitchOf(69), 440.0), 1.0)
    }

    @Test
    fun oscillatorShapesHaveTheExpectedHarmonics() {
        val expectations = mapOf(
            OscWave.TRIANGLE to { h: DoubleArray ->
                assertTrue("triangle even harmonic ${h[0]}", h[0] < 0.02)
                assertTrue("triangle h3 ${h[1]}", h[1] in 0.07..0.14)
                assertTrue("triangle h5 ${h[3]}", h[3] in 0.02..0.055)
            },
            OscWave.TRI_SAW to { h: DoubleArray ->
                assertTrue("tri/saw h2 ${h[0]}", h[0] in 0.22..0.38)
                assertTrue("tri/saw h3 ${h[1]}", h[1] in 0.15..0.27)
            },
            OscWave.SAWTOOTH to { h: DoubleArray ->
                assertTrue("saw h2 ${h[0]}", h[0] in 0.43..0.55)
                assertTrue("saw h3 ${h[1]}", h[1] in 0.28..0.38)
                assertTrue("saw h5 ${h[3]}", h[3] in 0.15..0.24)
            },
            OscWave.SQUARE to { h: DoubleArray ->
                assertTrue("square even harmonic ${h[0]}", h[0] < 0.02)
                assertTrue("square h3 ${h[1]}", h[1] in 0.30..0.36)
                assertTrue("square h5 ${h[3]}", h[3] in 0.17..0.23)
            },
            OscWave.PULSE_WIDE to { h: DoubleArray ->
                assertTrue("wide pulse h2 ${h[0]}", h[0] in 0.66..0.75)
                assertTrue("wide pulse h3 ${h[1]}", h[1] in 0.30..0.36)
            },
            OscWave.PULSE_NARROW to { h: DoubleArray ->
                assertTrue("narrow pulse h2 ${h[0]}", h[0] in 0.90..0.99)
                assertTrue("narrow pulse h3 ${h[1]}", h[1] in 0.82..0.92)
            }
        )

        for ((wave, check) in expectations) {
            val rig = Rig()
            rig.use(testPatch(wave = wave))
            rig.noteOn(57)
            val audio = rig.render(1.6f)
            val ratios = harmonicRatios(segment(audio, 0.35f, 1.35f), 220.0, 8, TEST_SAMPLE_RATE)
            check(ratios)
        }
    }

    @Test
    fun ladderCutoffSweepRaisesTheSpectralCentroid() {
        val centroids = ArrayList<Double>()
        for (cutoff in listOf(0.15f, 0.30f, 0.45f, 0.60f, 0.75f, 0.90f)) {
            val rig = Rig()
            rig.use(testPatch(wave = OscWave.SAWTOOTH, cutoff = cutoff))
            rig.noteOn(45)
            val audio = rig.render(1.2f)
            centroids.add(spectralCentroid(segment(audio, 0.5f, 1.2f), TEST_SAMPLE_RATE))
        }
        for (i in 1 until centroids.size) {
            assertTrue(
                "centroid did not rise: $centroids",
                centroids[i] > centroids[i - 1] + 2.0
            )
        }
        assertTrue("open filter should be much brighter: $centroids", centroids.last() > centroids.first() * 5.0)
    }

    private data class ResonanceBands(val low: Double, val peak: Double, val high: Double) {
        val peakToLow: Double get() = peak / low
        val peakToHigh: Double get() = peak / high
    }

    private fun resonanceBands(emphasis: Float): ResonanceBands {
        val rig = Rig()
        rig.use(
            testPatch(
                cutoff = 0.42f,
                emphasis = emphasis,
                noiseLevel = 0.7f,
                osc1Level = 0f,
                ampSustain = 1f
            )
        )
        rig.noteOn(57)
        val audio = rig.render(1.6f)
        val body = segment(audio, 0.6f, 1.5f)
        val fc = Maps.cutoffHz(0.42f).toDouble()
        return ResonanceBands(
            low = bandEnergy(body, fc * 0.25, fc * 0.5, TEST_SAMPLE_RATE),
            peak = bandEnergy(body, fc * 0.85, fc * 1.15, TEST_SAMPLE_RATE),
            high = bandEnergy(body, fc * 1.6, fc * 3.0, TEST_SAMPLE_RATE)
        )
    }

    @Test
    fun ladderResonanceBuildsAControlledPeak() {
        val dry = resonanceBands(0f)
        val medium = resonanceBands(0.7f)
        val hot = resonanceBands(0.9f)

        assertTrue("dry peak ratio ${dry.peakToLow}", dry.peakToLow < 1.0)
        assertTrue("medium peak ratio ${medium.peakToLow}", medium.peakToLow > 4.0)
        assertTrue("hot peak ratio ${hot.peakToLow}", hot.peakToLow > medium.peakToLow * 2.0)
        assertTrue("resonance should reject treble ${hot.peakToHigh}", hot.peakToHigh > 50.0)
    }

    @Test
    fun selfOscillationSingsWithoutRunningAway() {
        val p = testPatch(
            cutoff = 0.35f,
            emphasis = 1f,
            noiseLevel = 0.5f,
            osc1Level = 0f,
            drive = 0.3f,
            ampAttack = 0.01f,
            ampRelease = 0.4f
        )
        val rig = Rig()
        rig.use(p)
        rig.noteOn(60)
        rig.render(0.12f)
        rig.use(p.copy(noiseLevel = 0f))
        val tail = rig.render(2.8f)
        val late = segment(tail, 2.0f, 2.8f)
        val fc = Maps.cutoffHz(0.35f).toDouble()
        val pitch = estimatePitch(late, TEST_SAMPLE_RATE)

        assertTrue("self-oscillation produced invalid samples", allFinite(tail))
        assertTrue("self-oscillation peak ${peak(late)}", peak(late) < 0.3f)
        assertTrue("self-oscillation RMS ${rms(late)}", rms(late) > 0.005)
        assertTrue(
            "tail sang at $pitch Hz instead of cutoff $fc Hz",
            abs(cents(pitch, fc)) < 600.0
        )
    }

    @Test
    fun loudnessContourMatchesItsPanelTimes() {
        val p = testPatch(
            ampAttack = 0.557f,
            ampDecay = 0.528f,
            ampSustain = 0.5f,
            ampRelease = 0.384f
        )
        val attackLabel = Maps.ampAttackSeconds(p.ampAttack)
        val decayLabel = Maps.ampDecaySeconds(p.ampDecay)
        val releaseLabel = Maps.ampReleaseSeconds(p.ampRelease)

        val rig = Rig()
        rig.use(p)
        rig.noteOn(57)
        val on = rig.render(3.0f)
        rig.noteOff(57)
        val off = rig.render(3.0f)
        val m = measureEnvelope(on, off, TEST_SAMPLE_RATE)

        assertTrue("peak at %.3f s".format(m.peakSeconds), m.peakSeconds in 0.12..0.18)
        assertTrue("90%% attack at %.3f s".format(m.attackSeconds), m.attackSeconds in 0.06..0.16)
        assertTrue("decay %.3f s (knob %.3f s)".format(m.decaySeconds, decayLabel), m.decaySeconds in 0.15..0.30)
        assertEquals("sustain", 0.5, m.sustain, 0.04)
        assertTrue("release %.3f s (knob %.3f s)".format(m.releaseSeconds, releaseLabel), m.releaseSeconds in 0.14..0.25)
        assertTrue("attack label $attackLabel is calibrated", attackLabel in 0.12..0.18)
    }

    @Test
    fun oscillatorDriftIsSlowAndMusicallyBounded() {
        fun driftCents(drift: Float): List<Double> {
            val rig = Rig()
            rig.use(testPatch(drift = drift))
            rig.noteOn(69)
            val audio = rig.render(5.0f)
            val result = ArrayList<Double>()
            var start = 0.4f
            while (start + 0.3f <= 4.9f) {
                val p = estimatePitch(segment(audio, start, start + 0.3f), TEST_SAMPLE_RATE)
                result.add(cents(p, 440.0))
                start += 0.3f
            }
            return result
        }

        val still = driftCents(0f)
        val drifting = driftCents(1f)
        val maxStill = still.maxOf { abs(it) }
        val maxDrift = drifting.maxOf { abs(it) }
        val spread = drifting.maxOf { it } - drifting.minOf { it }

        assertTrue("undrifted tuning $still", maxStill < 0.3)
        assertTrue("drift exceeded a musical range: $drifting", maxDrift < 5.0)
        assertTrue("drift should audibly move: spread $spread", spread > 1.5)
    }

    @Test
    fun oscillatorTwoDetuneIsExact() {
        val rig = Rig()
        rig.use(testPatch(osc1Level = 0f, osc2Level = 0.9f, osc2Tune = 0.5f))
        rig.noteOn(57)
        val audio = rig.render(1.6f)
        val measured = estimatePitch(segment(audio, 0.4f, 1.3f), TEST_SAMPLE_RATE)
        val expected = 220.0 * 2.0.pow(3.5 / 12.0)
        assertTrue(
            "detuned oscillator at %.3f Hz, expected %.3f Hz".format(measured, expected),
            abs(cents(measured, expected)) < 3.0
        )
    }

    @Test
    fun glideMovesMonotonicallyAndArrivesInTune() {
        val rig = Rig()
        rig.use(testPatch(glide = 0.7f))
        rig.noteOn(48)
        rig.render(0.35f)
        rig.noteOn(69)
        val audio = rig.render(2.5f)
        val pitches = listOf(0.05f, 0.30f, 0.80f, 1.50f, 2.20f).map { t ->
            estimatePitch(segment(audio, t, t + 0.2f), TEST_SAMPLE_RATE)
        }

        for (i in 1 until pitches.size) {
            assertTrue("glide should rise monotonically: $pitches", pitches[i] > pitches[i - 1] - 1.0)
        }
        assertTrue("glide start ${pitches.first()} Hz", pitches.first() in 140.0..330.0)
        assertTrue("glide should arrive near A4: ${pitches.last()} Hz", abs(cents(pitches.last(), 440.0)) < 20.0)
    }

    @Test
    fun lastNotePrioritySelectsTheMostRecentHeldKey() {
        val rig = Rig()
        rig.use(testPatch())
        rig.noteOn(60)
        rig.render(0.4f)
        rig.noteOn(64)
        rig.render(0.4f)
        rig.noteOn(67)
        val g = rig.render(0.7f)
        assertEquals("G4", 392.0, estimatePitch(segment(g, 0.22f, 0.5f), TEST_SAMPLE_RATE), 1.5)
        rig.noteOff(67)
        val e = rig.render(0.7f)
        assertEquals("E4", 329.63, estimatePitch(segment(e, 0.22f, 0.5f), TEST_SAMPLE_RATE), 1.5)
        rig.noteOff(64)
        val c = rig.render(0.7f)
        assertEquals("C4", 261.63, estimatePitch(segment(c, 0.22f, 0.5f), TEST_SAMPLE_RATE), 1.5)
    }

    @Test
    fun noteOnAndOffDoNotProduceDiscontinuities() {
        val rig = Rig()
        rig.use(testPatch(ampAttack = 0.005f, ampRelease = 0.05f))
        rig.noteOn(57)
        val on = rig.render(0.8f)
        rig.noteOff(57)
        val off = rig.render(0.5f)

        val steadyStep = maxStep(on, (0.25f * TEST_SAMPLE_RATE).toInt(), (0.4f * TEST_SAMPLE_RATE).toInt())
        val onStep = maxStep(on, 0, (0.01f * TEST_SAMPLE_RATE).toInt())
        val offStep = maxStep(off, 0, (0.02f * TEST_SAMPLE_RATE).toInt())

        assertTrue("note-on jump $onStep vs steady $steadyStep", onStep < maxOf(0.05f, steadyStep * 8f))
        assertTrue("note-off jump $offStep vs steady $steadyStep", offStep < maxOf(0.02f, steadyStep * 3f))
        assertTrue("note-on should begin at silence", abs(on.first()) < 0.02f)
    }

    @Test
    fun retriggeringAHeldNoteDoesNotJump() {
        val rig = Rig()
        rig.use(testPatch(glide = 0.2f, ampAttack = 0.05f))
        rig.noteOn(69)
        rig.render(0.6f)
        val before = rig.render(0.01f)
        rig.noteOn(69)
        val after = rig.render(0.03f)
        val jump = abs(after.first() - before.last())
        assertTrue("retrigger discontinuity $jump", jump < 0.05f)
        assertTrue("retrigger produced invalid output", allFinite(after))
    }

    @Test
    fun everyFactoryPatchIsFiniteDcFreeAndHasHeadroom() {
        for (preset in FactoryPresets.all) {
            val rig = Rig()
            rig.use(preset.patch)
            rig.noteOn(48)
            val audio = rig.render(1.2f)
            val body = segment(audio, 0.5f, 1.2f)
            assertTrue("${preset.name}: NaN or Inf", allFinite(audio))
            assertTrue("${preset.name}: peak ${peak(audio)}", peak(audio) < 0.95f)
            assertTrue("${preset.name}: DC ${mean(body)}", abs(mean(body)) < 0.006)
        }
    }

    @Test
    fun summingAllSourcesStaysWithinHeadroom() {
        val rig = Rig()
        rig.use(
            testPatch(
                osc1Level = 1f,
                osc2Level = 1f,
                osc3Level = 1f,
                noiseLevel = 1f,
                drive = 0.8f,
                masterVolume = 1f
            )
        )
        rig.noteOn(48)
        val audio = rig.render(1.5f)
        assertTrue("summed signal is finite", allFinite(audio))
        assertTrue("summed signal peak ${peak(audio)}", peak(audio) < 0.95f)
        assertTrue("summed signal DC ${mean(segment(audio, 0.5f, 1.5f))}", abs(mean(segment(audio, 0.5f, 1.5f))) < 0.004)
    }
}
