package com.monarchd.synth.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import com.monarchd.synth.model.Maps
import com.monarchd.synth.model.OscRange
import com.monarchd.synth.model.Patch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * The complete instrument: a monophonic, last-note-priority voice rendered in
 * real time into an AudioTrack using the device's native sample rate and a
 * low-latency buffer. No samples are loaded; every sound is synthesised.
 */
class SynthEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile private var patch: Patch = Patch()
    @Volatile private var modWheel: Float = 0f
    @Volatile private var pitchBend: Float = 0f
    @Volatile private var muted: Boolean = false
    @Volatile private var running: Boolean = false

    private var worker: Thread? = null
    private var audioTrack: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null

    private val events = ConcurrentLinkedQueue<Event>()

    @Volatile var sampleRate: Int = 48_000
        private set

    @Volatile var bufferFrames: Int = 0
        private set

    val latencyMs: Float
        get() = if (sampleRate > 0) bufferFrames * 1000f / sampleRate else 0f

    private val scopeLock = ReentrantLock()
    private val scopeBuffer = FloatArray(SCOPE_SIZE)

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        muted = change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
    }

    fun start() {
        if (running) return
        running = true
        requestFocus()
        worker = Thread({ audioLoop() }, "monarch-audio").apply { isDaemon = true }.also { it.start() }
    }

    fun stop() {
        running = false
        runCatching { audioTrack?.pause() }
        worker?.let { runCatching { it.join(600) } }
        worker = null
        abandonFocus()
        muteRampReset()
    }

    fun setPatch(newPatch: Patch) {
        patch = newPatch
    }

    fun setModWheel(value: Float) {
        modWheel = value.coerceIn(0f, 1f)
    }

    fun setPitchBend(value: Float) {
        pitchBend = value.coerceIn(-1f, 1f)
    }

    fun noteOn(note: Int) = events.add(Event.On(note.coerceIn(0, 127)))

    fun noteOff(note: Int) = events.add(Event.Off(note.coerceIn(0, 127)))

    fun allNotesOff() = events.add(Event.AllOff)

    /** Non-blocking copy of the most recent output window, for the scope display. */
    fun copyScope(destination: FloatArray) {
        if (!scopeLock.tryLock()) return
        try {
            System.arraycopy(scopeBuffer, 0, destination, 0, minOf(destination.size, scopeBuffer.size))
        } finally {
            scopeLock.unlock()
        }
    }

    private fun requestFocus() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .setWillPauseWhenDucked(false)
            .build()
        focusRequest = request
        runCatching { audioManager.requestAudioFocus(request) }
        muted = false
    }

    private fun abandonFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        focusRequest = null
    }

    private fun muteRampReset() {
        muted = false
    }

    private fun audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull()?.takeIf { it in 8_000..192_000 } ?: 48_000
        val nativeFrames = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            ?.toIntOrNull()?.takeIf { it in 32..4_096 } ?: 192
        sampleRate = rate

        val minBytes = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        if (minBytes <= 0) {
            running = false
            return
        }
        val frames = maxOf(minBytes / 8, nativeFrames * 2, 192)
        bufferFrames = frames

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(frames * 2 * 4)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        }.getOrNull()

        if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
            track?.release()
            running = false
            return
        }

        audioTrack = track
        track.play()

        val voice = Voice(rate.toFloat())
        val block = 256
        val output = FloatArray(block * 2)
        val staging = FloatArray(SCOPE_SIZE)
        var scopeWrite = 0
        var sampleIndex = 0
        var scopeCounter = 0

        try {
            while (running) {
                while (true) {
                    val event = events.poll() ?: break
                    when (event) {
                        is Event.On -> voice.noteOn(event.note)
                        is Event.Off -> voice.noteOff(event.note)
                        Event.AllOff -> voice.allNotesOff()
                    }
                }
                voice.setPatch(patch)
                voice.setWheels(modWheel, pitchBend)
                voice.setMuted(muted)

                var i = 0
                while (i < block) {
                    val sample = voice.render()
                    output[i * 2] = sample
                    output[i * 2 + 1] = sample
                    if ((sampleIndex and 3) == 0) {
                        staging[scopeWrite++] = sample
                        if (scopeWrite >= SCOPE_SIZE) {
                            scopeWrite = 0
                            scopeCounter++
                            if (scopeLock.tryLock()) {
                                try {
                                    System.arraycopy(staging, 0, scopeBuffer, 0, SCOPE_SIZE)
                                } finally {
                                    scopeLock.unlock()
                                }
                            }
                        }
                    }
                    sampleIndex++
                    i++
                }

                var offset = 0
                while (offset < output.size && running) {
                    val written = track.write(output, offset, output.size - offset, AudioTrack.WRITE_BLOCKING)
                    if (written < 0) {
                        running = false
                        break
                    }
                    offset += written
                }
            }
        } finally {
            runCatching { track.stop() }
            track.release()
            audioTrack = null
            voice.resetAudioState()
        }
    }

    private sealed class Event {
        data class On(val note: Int) : Event()
        data class Off(val note: Int) : Event()
        data object AllOff : Event()
    }

    /**
     * A single monophonic voice. Frequency, cutoff and modulation are updated at
     * control rate; oscillators and envelopes run per sample.
     */
    private class Voice(private val sampleRate: Float) {
        private val osc1 = Oscillator(sampleRate)
        private val osc2 = Oscillator(sampleRate)
        private val osc3 = Oscillator(sampleRate)
        private val ladder = MoogLadder(sampleRate)
        private val filterEnv = Adsr(sampleRate)
        private val ampEnv = Adsr(sampleRate)
        private val noise = NoiseSource()
        private val randomMod = RandomModulator(sampleRate)

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

            val driven = tanhFast(mix * preGain)
            val filtered = ladder.process(driven)
            var out = filtered * ampEnvelope * masterGain
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
}
