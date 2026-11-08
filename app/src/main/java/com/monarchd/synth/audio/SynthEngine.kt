package com.monarchd.synth.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import com.monarchd.synth.model.Patch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock

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

        val voice = SynthVoice(rate.toFloat())
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

}
