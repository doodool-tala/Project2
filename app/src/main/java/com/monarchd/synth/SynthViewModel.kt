package com.monarchd.synth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monarchd.synth.audio.SynthEngine
import com.monarchd.synth.model.AppSettings
import com.monarchd.synth.model.FactoryPresets
import com.monarchd.synth.model.Patch
import com.monarchd.synth.model.PatchRepository
import com.monarchd.synth.model.Preset
import com.monarchd.synth.model.PresetCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class SynthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PatchRepository(application)
    private val engine = SynthEngine(application)

    val factoryPresets: List<Preset> = FactoryPresets.all

    var patch by mutableStateOf(repository.loadLastPatch())
        private set

    var userPresets by mutableStateOf(repository.loadUserPresets())
        private set

    var settings by mutableStateOf(repository.loadSettings())
        private set

    var showIntro by mutableStateOf(repository.isFirstRun())
        private set

    private var saveJob: Job? = null
    private var audioStarted = false

    init {
        engine.setPatch(patch)
    }

    fun startAudio() {
        if (!audioStarted) {
            engine.start()
            audioStarted = true
        }
    }

    fun stopAudio() {
        engine.allNotesOff()
        if (audioStarted) {
            engine.stop()
            audioStarted = false
        }
    }

    fun updatePatch(transform: (Patch) -> Patch) {
        val updated = transform(patch)
        if (updated == patch) return
        patch = updated
        engine.setPatch(updated)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(450)
            repository.saveLastPatch(patch)
        }
    }

    fun loadPatch(newPatch: Patch) {
        patch = newPatch.copy()
        engine.setPatch(patch)
        engine.allNotesOff()
        repository.saveLastPatch(patch)
    }

    fun saveAs(name: String) {
        repository.saveUserPreset(name, patch)
        val named = patch.copy(name = name.trim().take(40), category = PresetCategory.USER)
        patch = named
        engine.setPatch(named)
        repository.saveLastPatch(named)
        userPresets = repository.loadUserPresets()
    }

    fun deletePreset(name: String) {
        repository.deleteUserPreset(name)
        userPresets = repository.loadUserPresets()
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        repository.saveSettings(settings)
    }

    fun dismissIntro() {
        repository.markIntroSeen()
        showIntro = false
    }

    fun panic() {
        engine.allNotesOff()
    }

    fun noteOn(note: Int) = engine.noteOn(note)

    fun noteOff(note: Int) = engine.noteOff(note)

    fun setPitchBend(value: Float) = engine.setPitchBend(value)

    fun setModWheel(value: Float) = engine.setModWheel(value)

    fun copyScope(destination: FloatArray) = engine.copyScope(destination)

    val audioSummary: String
        get() {
            val rate = engine.sampleRate / 1000f
            val latency = engine.latencyMs
            return if (engine.bufferFrames == 0) {
                "48.0 kHz · internal 32-bit float engine"
            } else {
                "%.1f kHz · %.1f ms buffer · 32-bit float".format(rate, latency)
            }
        }

    /** A musical randomiser: keeps the patch playable rather than chaotic. */
    fun randomize() {
        val random = Random(System.nanoTime())
        fun chance(p: Float) = random.nextFloat() < p
        val categories = PresetCategory.entries.filter { it != PresetCategory.USER }
        val base = FactoryPresets.all.random(random).patch
        val waves = listOf(
            com.monarchd.synth.model.OscWave.SAWTOOTH,
            com.monarchd.synth.model.OscWave.SQUARE,
            com.monarchd.synth.model.OscWave.TRIANGLE,
            com.monarchd.synth.model.OscWave.TRI_SAW,
            com.monarchd.synth.model.OscWave.PULSE_WIDE
        )
        val randomPatch = base.copy(
            name = "Random Patch",
            category = categories.random(random),
            description = "Generated on the spot",
            osc1Wave = waves.random(random),
            osc2Wave = waves.random(random),
            osc2Level = if (chance(0.8f)) random.nextFloat() * 0.7f else 0f,
            osc2Tune = (random.nextFloat() - 0.5f) * 0.25f,
            osc3Wave = waves.random(random),
            osc3Level = if (chance(0.3f)) random.nextFloat() * 0.35f else 0f,
            noiseLevel = if (chance(0.2f)) random.nextFloat() * 0.4f else 0f,
            cutoff = random.nextFloat() * 0.75f + 0.12f,
            emphasis = random.nextFloat() * 0.8f,
            contourAmount = random.nextFloat() * 0.8f,
            filterAttack = random.nextFloat() * 0.65f,
            filterDecay = random.nextFloat() * 0.8f,
            filterSustain = random.nextFloat() * 0.7f,
            ampAttack = random.nextFloat() * 0.55f,
            ampDecay = random.nextFloat() * 0.8f,
            ampSustain = 0.35f + random.nextFloat() * 0.6f,
            ampRelease = random.nextFloat() * 0.6f,
            glide = if (chance(0.35f)) random.nextFloat() * 0.3f else 0f,
            drive = random.nextFloat() * 0.55f,
            modMix = random.nextFloat()
        )
        loadPatch(randomPatch)
    }

    companion object {
        fun factory(application: Application) = viewModelFactory {
            initializer { SynthViewModel(application) }
        }
    }
}
