package com.monarchd.synth.audio

import com.monarchd.synth.model.Maps
import com.monarchd.synth.model.OscRange
import com.monarchd.synth.model.Patch
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * A single monophonic voice. Frequency, cutoff and modulation are updated at
 * control rate; oscillators and envelopes run per sample. This class has no
 * Android dependencies, so the exact engine path can be rendered offline.
 */
internal class SynthVoice(private val sampleRate: Float) {
    private val osc1 = Oscillator(sampleRate)
    private val osc2 = Oscillator(sampleRate)
    private val osc3 = Oscillator(sampleRate)
    private val ladder = MoogLadder(sampleRate)
    private val filterEnv = Adsr(sampleRate)
    private val ampEnv = Adsr(sampleRate)
    private val noise = NoiseSource()
    private val randomMod = RandomModulator(sampleRate)
    private val dcBlock = DcBlocker(sampleRate)

    private var patch: Patch = Patch()
    private var applied: Patch? = null

    private val held = IntArray(16)
    private var heldCount = 0
    private var targetNote = 60.0
    private var currentNote = 60.0
    private var hasPlayed = false

    private var modWheel = 0f
    private var modWheelSmooth = 0f
    private var bend = 0f
    private var bendSmooth = 0f
    private var muted = false
    private var muteGain = 1f

    private var controlCounter = CONTROL_INTERVAL
    private var filterEnvValue = 0f
    private var lastOsc3 = 0f
    private var modSource = 0f
    private var driftPhase1 = 0.13
    private var driftPhase2 = 0.47
    private var driftPhase3 = 0.81
    private var preGain = 1f
    private var masterGain = 0.75f

    fun setPatch(newPatch: Patch) {
        patch = newPatch
    }

    fun setWheels(mod: Float, pitchBend: Float) {
        modWheel = mod
        bend = pitchBend
    }

    fun setMuted(value: Boolean) {
        muted = value
    }

    fun noteOn(note: Int) {
        var alreadyHeld = false
        for (i in 0 until heldCount) {
            if (held[i] == note) alreadyHeld = true
        }
        if (!alreadyHeld) {
            if (heldCount < held.size) {
                held[heldCount++] = note
            } else {
                var i = 1
                while (i < held.size) {
                    held[i - 1] = held[i]
                    i++
                }
                held[held.size - 1] = note
            }
        }
        targetNote = note.toDouble()
        if (!hasPlayed) {
            currentNote = targetNote
            hasPlayed = true
        }
        filterEnv.retrigger()
        ampEnv.retrigger()
    }

    fun noteOff(note: Int) {
        var index = -1
        for (i in 0 until heldCount) {
            if (held[i] == note) {
                index = i
                break
            }
        }
        if (index < 0) return
        var i = index
        while (i < heldCount - 1) {
            held[i] = held[i + 1]
            i++
        }
        heldCount--
        if (heldCount > 0) {
            targetNote = held[heldCount - 1].toDouble()
        } else {
            filterEnv.release()
            ampEnv.release()
        }
    }

    fun allNotesOff() {
        heldCount = 0
        filterEnv.release()
        ampEnv.release()
    }

    fun resetAudioState() {
        filterEnv.reset()
        ampEnv.reset()
        ladder.reset()
        dcBlock.reset()
        randomMod.reset()
        osc1.resetPhase(0.0)
        osc2.resetPhase(0.0)
        osc3.resetPhase(0.0)
    }

    fun render(): Float {
        val p = patch
        if (applied !== p) {
            applied = p
            applyPatch(p)
        }
        if (--controlCounter <= 0) {
            controlCounter = CONTROL_INTERVAL
            updateControl(p)
        }

        val filterEnvelope = filterEnv.process()
        filterEnvValue = filterEnvelope
        val ampEnvelope = ampEnv.process()

        osc1.advance()
        osc2.advance()
        osc3.advance()
        val v1 = osc1.sample(p.osc1Wave)
        val v2 = osc2.sample(p.osc2Wave)
        val v3 = osc3.sample(p.osc3Wave)
        lastOsc3 = v3

        val mix = v1 * p.osc1Level +
            v2 * p.osc2Level +
            v3 * p.osc3Level +
            noise.next() * p.noiseLevel * 0.8f

        // Drive blends into saturation instead of colouring the signal at zero.
        val saturation = p.drive.coerceIn(0f, 1f)
        val driven = mix * (1f - saturation) + tanhFast(mix * preGain) * saturation
        val filtered = ladder.process(driven)
        var out = filtered * ampEnvelope * masterGain
        out = dcBlock.process(out)
        out = tanhFast(out * 1.15f)

        val targetMute = if (muted) 0f else 1f
        muteGain += (targetMute - muteGain) * 0.0009f
        out *= muteGain

        if (!out.isFinite()) {
            resetAudioState()
            return 0f
        }
        return out
    }

    private fun applyPatch(p: Patch) {
        filterEnv.setParams(
            Maps.filterAttackSeconds(p.filterAttack),
            Maps.filterDecaySeconds(p.filterDecay),
            p.filterSustain,
            Maps.filterReleaseSeconds(p.filterDecay),
            p.filterDecayOn
        )
        ampEnv.setParams(
            Maps.ampAttackSeconds(p.ampAttack),
            Maps.ampDecaySeconds(p.ampDecay),
            p.ampSustain,
            Maps.ampReleaseSeconds(p.ampRelease),
            true
        )
        ladder.resonance = p.emphasis
        preGain = 1f + p.drive * 4f
        // Compensate loudness so the drive control adds character, not volume jumps.
        masterGain = 0.92f * p.masterVolume.pow(1.5f) / (1f + p.drive * 1.1f)
    }

    private fun updateControl(p: Patch) {
        modWheelSmooth += (modWheel - modWheelSmooth) * 0.08f
        bendSmooth += (bend - bendSmooth) * 0.10f

        val dt = CONTROL_INTERVAL / sampleRate
        val glideSeconds = Maps.glideSeconds(p.glide)
        val glideCoef = if (glideSeconds < 0.004f) {
            1f
        } else {
            (1.0 - exp(-dt / (glideSeconds * 0.35))).toFloat()
        }
        currentNote += (targetNote - currentNote) * glideCoef

        val modNoise = randomMod.next()
        modSource = lastOsc3 * (1f - p.modMix) + modNoise * p.modMix
        val vibrato = modWheelSmooth * 0.45f * modSource
        val basePitch = currentNote +
            Maps.masterTuneSemitones(p.masterTune) +
            bendSmooth * 2f +
            vibrato

        val driftScale = p.drift * 0.035f
        driftPhase1 = (driftPhase1 + 0.11 * dt) % 1.0
        driftPhase2 = (driftPhase2 + 0.083 * dt) % 1.0
        driftPhase3 = (driftPhase3 + 0.137 * dt) % 1.0
        val drift1 = sin(driftPhase1 * TWO_PI).toFloat() * driftScale
        val drift2 = sin(driftPhase2 * TWO_PI).toFloat() * driftScale
        val drift3 = sin(driftPhase3 * TWO_PI).toFloat() * driftScale

        osc1.setFrequency(midiToHz(basePitch + p.osc1Range.semitones + drift1))
        osc2.setFrequency(
            midiToHz(basePitch + p.osc2Range.semitones + Maps.oscTuneSemitones(p.osc2Tune) + drift2)
        )
        if (p.osc3Range == OscRange.LO) {
            osc3.setFrequency(Maps.osc3RateHz(p.osc3Tune).toDouble())
        } else {
            osc3.setFrequency(
                midiToHz(basePitch + p.osc3Range.semitones + Maps.oscTuneSemitones(p.osc3Tune) + drift3)
            )
        }

        val baseCutoff = Maps.cutoffHz(p.cutoff)
        val envelopeOctaves = p.contourAmount * filterEnvValue * 5f
        val trackingOctaves = p.keyTrack.amount * (currentNote - 60.0).toFloat() / 12f
        var modulationOctaves = modWheelSmooth * modSource * 0.85f
        if (p.osc3ToFilter) {
            modulationOctaves += lastOsc3 * (0.35f + 0.65f * modWheelSmooth) * 2.2f
        }
        val cutoff = baseCutoff * 2.0.pow(
            (envelopeOctaves + trackingOctaves + modulationOctaves).toDouble()
        )
        ladder.setCutoff(cutoff.coerceIn(18.0, sampleRate * 0.42).toFloat())
    }

    private companion object {
        const val CONTROL_INTERVAL = 8
    }
}
