package com.monarchd.synth.audio

import com.monarchd.synth.model.OscWave
import org.junit.Assert.assertTrue
import org.junit.Test

class DspTest {

    private val sampleRate = 48_000f

    @Test
    fun oscillatorsProduceBoundedAudio() {
        for (wave in OscWave.entries) {
            val osc = Oscillator(sampleRate)
            osc.setFrequency(220.0)
            repeat(2_000) { osc.advance() }
            var peak = 0f
            repeat(24_000) {
                osc.advance()
                val sample = osc.sample(wave)
                assertTrue("$wave produced a non-finite sample", sample.isFinite())
                peak = maxOf(peak, kotlin.math.abs(sample))
            }
            assertTrue("$wave peak too small: $peak", peak > 0.3f)
            assertTrue("$wave peak too large: $peak", peak <= 1.6f)
        }
    }

    @Test
    fun ladderFilterIsStableAtMaximumResonance() {
        val ladder = MoogLadder(sampleRate)
        ladder.resonance = 1f
        ladder.setCutoff(900f)
        var peak = 0f
        repeat(96_000) { i ->
            val input = if (i < 480) 0.9f else 0f
            val out = ladder.process(input)
            assertTrue("filter produced a non-finite sample", out.isFinite())
            peak = maxOf(peak, kotlin.math.abs(out))
        }
        assertTrue("filter exploded: $peak", peak < 2.5f)
    }

    @Test
    fun envelopeMovesThroughAttackSustainAndRelease() {
        val env = Adsr(sampleRate)
        env.setParams(0.01f, 0.05f, 0.4f, 0.05f, true)
        env.retrigger()
        repeat(4_800) { env.process() }
        assertTrue("envelope never reached sustain", env.value in 0.35f..0.45f)
        env.release()
        repeat(48_000) { env.process() }
        assertTrue("envelope did not release", env.isIdle && env.value == 0f)
    }
}
