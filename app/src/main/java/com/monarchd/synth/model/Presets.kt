package com.monarchd.synth.model

/**
 * Hand-tuned starting points. Each patch is deliberately musical rather than
 * extreme: classic basses, singing leads, plucked keys, evolving pads and a few
 * sound-design experiments that show off the ladder filter.
 */
object FactoryPresets {

    private fun patch(
        name: String,
        category: PresetCategory,
        description: String,
        base: Patch = Patch()
    ) = Preset(name, category, description, base.copy(name = name, category = category, description = description))

    val all: List<Preset> = listOf(
        patch(
            name = "Model One",
            category = PresetCategory.BASS,
            description = "The benchmark: round saw bass with a fast, greasy filter snap.",
            base = Patch(
                osc1Range = OscRange.SIXTEEN, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.82f,
                osc2Range = OscRange.SIXTEEN, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.48f, osc2Tune = 0.035f,
                osc3Range = OscRange.LO, osc3Wave = OscWave.TRIANGLE, osc3Level = 0f,
                cutoff = 0.30f, emphasis = 0.22f, contourAmount = 0.52f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0.0f, filterDecay = 0.36f, filterSustain = 0.12f,
                ampAttack = 0.0f, ampDecay = 0.38f, ampSustain = 0.72f, ampRelease = 0.12f,
                drive = 0.22f, glide = 0.02f, masterVolume = 0.82f
            )
        ),
        patch(
            name = "Rubber Funk",
            category = PresetCategory.BASS,
            description = "Squelchy square bass. Dig in with short, syncopated notes.",
            base = Patch(
                osc1Range = OscRange.SIXTEEN, osc1Wave = OscWave.SQUARE, osc1Level = 0.7f,
                osc2Range = OscRange.SIXTEEN, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.42f, osc2Tune = -0.05f,
                osc3Range = OscRange.LO, osc3Wave = OscWave.TRIANGLE,
                cutoff = 0.24f, emphasis = 0.58f, contourAmount = 0.58f, keyTrack = KeyTrack.THIRD,
                filterAttack = 0.0f, filterDecay = 0.26f, filterSustain = 0f,
                ampAttack = 0.0f, ampDecay = 0.32f, ampSustain = 0.55f, ampRelease = 0.10f,
                drive = 0.30f, glide = 0.03f
            )
        ),
        patch(
            name = "Sub Monarch",
            category = PresetCategory.BASS,
            description = "A deep triangle foundation with just enough edge to cut through.",
            base = Patch(
                osc1Range = OscRange.THIRTY_TWO, osc1Wave = OscWave.TRIANGLE, osc1Level = 0.95f,
                osc2Range = OscRange.SIXTEEN, osc2Wave = OscWave.SQUARE, osc2Level = 0.22f, osc2Tune = 0f,
                osc3Range = OscRange.LO, osc3Wave = OscWave.TRIANGLE,
                cutoff = 0.17f, emphasis = 0.08f, contourAmount = 0.30f, keyTrack = KeyTrack.OFF,
                filterAttack = 0f, filterDecay = 0.5f, filterSustain = 0.30f,
                ampAttack = 0.01f, ampDecay = 0.5f, ampSustain = 0.85f, ampRelease = 0.18f,
                drive = 0.10f, masterVolume = 0.86f
            )
        ),
        patch(
            name = "Acid Reign",
            category = PresetCategory.BASS,
            description = "Resonant saw with a biting, zero-sustain filter sweep.",
            base = Patch(
                osc1Range = OscRange.SIXTEEN, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.85f,
                osc2Level = 0f, osc3Level = 0f,
                cutoff = 0.20f, emphasis = 0.84f, contourAmount = 0.62f, keyTrack = KeyTrack.THIRD,
                filterAttack = 0f, filterDecay = 0.22f, filterSustain = 0f,
                ampAttack = 0f, ampDecay = 0.30f, ampSustain = 0.80f, ampRelease = 0.08f,
                drive = 0.42f, glide = 0.12f
            )
        ),
        patch(
            name = "Growl Cabinet",
            category = PresetCategory.BASS,
            description = "Overdriven three-oscillator bass through the ladder at full tilt.",
            base = Patch(
                osc1Range = OscRange.SIXTEEN, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.72f,
                osc2Range = OscRange.SIXTEEN, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.62f, osc2Tune = -0.10f,
                osc3Range = OscRange.EIGHT, osc3Wave = OscWave.SQUARE, osc3Level = 0.30f, osc3Tune = -0.05f,
                cutoff = 0.27f, emphasis = 0.42f, contourAmount = 0.48f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0f, filterDecay = 0.30f, filterSustain = 0.22f,
                ampAttack = 0f, ampDecay = 0.35f, ampSustain = 0.78f, ampRelease = 0.12f,
                drive = 0.62f, glide = 0.02f
            )
        ),
        patch(
            name = "Whistle King",
            category = PresetCategory.LEAD,
            description = "Pure triangle lead with a gliding, vocal quality. Add the mod wheel.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.TRIANGLE, osc1Level = 0.9f,
                osc2Range = OscRange.FOUR, osc2Wave = OscWave.TRIANGLE, osc2Level = 0.18f, osc2Tune = 0.01f,
                osc3Level = 0f,
                cutoff = 0.78f, emphasis = 0.68f, contourAmount = 0.22f, keyTrack = KeyTrack.FULL,
                filterAttack = 0.02f, filterDecay = 0.4f, filterSustain = 0.55f,
                ampAttack = 0.03f, ampDecay = 0.45f, ampSustain = 0.82f, ampRelease = 0.22f,
                glide = 0.18f, drive = 0.18f
            )
        ),
        patch(
            name = "Brass Solo",
            category = PresetCategory.LEAD,
            description = "The classic brass swell: slow filter attack, three detuned saws.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.7f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.55f, osc2Tune = 0.02f,
                osc3Range = OscRange.SIXTEEN, osc3Wave = OscWave.SAWTOOTH, osc3Level = 0.35f, osc3Tune = -0.03f,
                cutoff = 0.42f, emphasis = 0.30f, contourAmount = 0.68f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0.12f, filterDecay = 0.38f, filterSustain = 0.48f,
                ampAttack = 0.04f, ampDecay = 0.5f, ampSustain = 0.85f, ampRelease = 0.24f,
                drive = 0.26f, glide = 0.05f
            )
        ),
        patch(
            name = "Fifth Avenue",
            category = PresetCategory.LEAD,
            description = "Saw and square stacked a fifth apart. Instant 1970s heroics.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.68f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.42f, osc2Tune = 1f,
                osc3Range = OscRange.FOUR, osc3Wave = OscWave.SQUARE, osc3Level = 0.24f,
                cutoff = 0.56f, emphasis = 0.30f, contourAmount = 0.35f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0.03f, filterDecay = 0.4f, filterSustain = 0.45f,
                ampAttack = 0.01f, ampDecay = 0.5f, ampSustain = 0.8f, ampRelease = 0.18f,
                drive = 0.35f, glide = 0.08f
            )
        ),
        patch(
            name = "Fuzz Sermon",
            category = PresetCategory.LEAD,
            description = "Aggressive pulse lead with the filter pushed into self-oscillation.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.PULSE_WIDE, osc1Level = 0.85f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.35f, osc2Tune = -0.08f,
                osc3Level = 0f,
                cutoff = 0.38f, emphasis = 0.72f, contourAmount = 0.55f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0f, filterDecay = 0.30f, filterSustain = 0.35f,
                ampAttack = 0f, ampDecay = 0.45f, ampSustain = 0.85f, ampRelease = 0.12f,
                drive = 0.72f, glide = 0.04f, masterVolume = 0.72f
            )
        ),
        patch(
            name = "Nose Flute",
            category = PresetCategory.LEAD,
            description = "Hollow, reedy and slightly unstable. The mod wheel is your breath.",
            base = Patch(
                osc1Range = OscRange.FOUR, osc1Wave = OscWave.TRI_SAW, osc1Level = 0.85f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.TRIANGLE, osc2Level = 0.22f, osc2Tune = 0.015f,
                osc3Range = OscRange.LO, osc3Wave = OscWave.TRIANGLE, osc3Level = 0f, osc3ToFilter = true,
                cutoff = 0.70f, emphasis = 0.80f, contourAmount = 0.22f, keyTrack = KeyTrack.FULL,
                filterAttack = 0.08f, filterDecay = 0.5f, filterSustain = 0.6f,
                ampAttack = 0.06f, ampDecay = 0.5f, ampSustain = 0.85f, ampRelease = 0.28f,
                glide = 0.20f, drive = 0.12f, drift = 0.6f
            )
        ),
        patch(
            name = "Vintage Clav",
            category = PresetCategory.KEYS,
            description = "Tight pulse pluck with a woody filter thump. Try short, percussive lines.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.PULSE_WIDE, osc1Level = 0.85f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.PULSE_NARROW, osc2Level = 0.30f, osc2Tune = 0.02f,
                osc3Level = 0f,
                cutoff = 0.40f, emphasis = 0.52f, contourAmount = 0.68f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0f, filterDecay = 0.20f, filterSustain = 0f,
                ampAttack = 0f, ampDecay = 0.28f, ampSustain = 0.20f, ampRelease = 0.10f,
                drive = 0.30f, masterVolume = 0.8f
            )
        ),
        patch(
            name = "Warm Electric",
            category = PresetCategory.KEYS,
            description = "Mellow triangle keys with a slow, singing decay. Velvet for chords.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.TRIANGLE, osc1Level = 0.9f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.TRI_SAW, osc2Level = 0.18f, osc2Tune = 0.01f,
                osc3Level = 0f,
                cutoff = 0.47f, emphasis = 0.16f, contourAmount = 0.30f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0.01f, filterDecay = 0.58f, filterSustain = 0.22f,
                ampAttack = 0.005f, ampDecay = 0.80f, ampSustain = 0.22f, ampRelease = 0.42f,
                drive = 0.12f, masterVolume = 0.86f
            )
        ),
        patch(
            name = "Hollow Organ",
            category = PresetCategory.KEYS,
            description = "Stopped flute pipes made from stacked squares. Hold a chord and listen.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.SQUARE, osc1Level = 0.7f,
                osc2Range = OscRange.FOUR, osc2Wave = OscWave.SQUARE, osc2Level = 0.34f, osc2Tune = 0f,
                osc3Range = OscRange.SIXTEEN, osc3Wave = OscWave.SQUARE, osc3Level = 0.22f,
                osc3ToFilter = false,
                cutoff = 0.64f, emphasis = 0.05f, contourAmount = 0.05f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0f, filterDecay = 0.4f, filterSustain = 0.9f,
                ampAttack = 0.005f, ampDecay = 0.20f, ampSustain = 1f, ampRelease = 0.15f,
                drive = 0.10f, masterVolume = 0.72f
            )
        ),
        patch(
            name = "Celestial Choir",
            category = PresetCategory.PAD,
            description = "Slow, drifting saw pad. Let the analog drift blur the oscillators.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.62f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.52f, osc2Tune = 0.06f,
                osc3Range = OscRange.SIXTEEN, osc3Wave = OscWave.TRIANGLE, osc3Level = 0.30f, osc3Tune = -0.02f,
                cutoff = 0.42f, emphasis = 0.24f, contourAmount = 0.34f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0.38f, filterDecay = 0.6f, filterSustain = 0.52f,
                ampAttack = 0.48f, ampDecay = 0.7f, ampSustain = 0.82f, ampRelease = 0.62f,
                drive = 0.16f, glide = 0.14f, drift = 0.65f, masterVolume = 0.7f
            )
        ),
        patch(
            name = "Slow Bloom",
            category = PresetCategory.PAD,
            description = "Opens like a curtain: long filter and loudness swells over a dark saw bed.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.TRI_SAW, osc1Level = 0.7f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.SAWTOOTH, osc2Level = 0.35f, osc2Tune = -0.04f,
                osc3Level = 0f,
                cutoff = 0.30f, emphasis = 0.42f, contourAmount = 0.60f, keyTrack = KeyTrack.THIRD,
                filterAttack = 0.62f, filterDecay = 0.7f, filterSustain = 0.65f,
                ampAttack = 0.62f, ampDecay = 0.7f, ampSustain = 0.9f, ampRelease = 0.72f,
                drive = 0.22f, glide = 0.10f, drift = 0.5f, masterVolume = 0.72f
            )
        ),
        patch(
            name = "Glass Cathedral",
            category = PresetCategory.PAD,
            description = "Bright, narrow pulses with a long tail. Sounds enormous with reverb.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.PULSE_NARROW, osc1Level = 0.62f,
                osc2Range = OscRange.FOUR, osc2Wave = OscWave.PULSE_WIDE, osc2Level = 0.30f, osc2Tune = 0.03f,
                osc3Range = OscRange.EIGHT, osc3Wave = OscWave.TRIANGLE, osc3Level = 0.22f, osc3Tune = 0.02f,
                cutoff = 0.62f, emphasis = 0.52f, contourAmount = 0.30f, keyTrack = KeyTrack.FULL,
                filterAttack = 0.22f, filterDecay = 0.6f, filterSustain = 0.5f,
                ampAttack = 0.25f, ampDecay = 0.7f, ampSustain = 0.75f, ampRelease = 0.80f,
                drive = 0.12f, drift = 0.5f, masterVolume = 0.66f
            )
        ),
        patch(
            name = "Cosmic Sweep",
            category = PresetCategory.FX,
            description = "Noise and saw rising through a resonant ladder. Hold a low note and wait.",
            base = Patch(
                osc1Range = OscRange.SIXTEEN, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.42f,
                osc2Range = OscRange.EIGHT, osc2Wave = OscWave.TRIANGLE, osc2Level = 0.25f, osc2Tune = 0.12f,
                osc3Level = 0f, noiseLevel = 0.55f,
                cutoff = 0.16f, emphasis = 0.72f, contourAmount = 0.82f, keyTrack = KeyTrack.OFF,
                filterAttack = 0.78f, filterDecay = 0.9f, filterSustain = 0.15f,
                ampAttack = 0.45f, ampDecay = 0.9f, ampSustain = 0.9f, ampRelease = 0.55f,
                drive = 0.30f, glide = 0.10f, drift = 0.85f, masterVolume = 0.7f
            )
        ),
        patch(
            name = "Deep Space Drone",
            category = PresetCategory.FX,
            description = "Oscillator three becomes a slow filter modulator. A living, breathing drone.",
            base = Patch(
                osc1Range = OscRange.THIRTY_TWO, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.55f,
                osc2Range = OscRange.THIRTY_TWO, osc2Wave = OscWave.TRIANGLE, osc2Level = 0.40f, osc2Tune = 0.03f,
                osc3Range = OscRange.LO, osc3Wave = OscWave.TRIANGLE, osc3Level = 0f, osc3Tune = -0.25f, osc3ToFilter = true,
                noiseLevel = 0.10f,
                cutoff = 0.22f, emphasis = 0.55f, contourAmount = 0.18f, keyTrack = KeyTrack.OFF,
                filterAttack = 0.4f, filterDecay = 0.8f, filterSustain = 0.6f,
                ampAttack = 0.32f, ampDecay = 0.8f, ampSustain = 1f, ampRelease = 0.85f,
                drive = 0.24f, glide = 0.35f, drift = 0.9f, masterVolume = 0.68f
            )
        ),
        patch(
            name = "Radio Static",
            category = PresetCategory.FX,
            description = "Filtered noise with a metallic edge. Mod wheel it into a storm.",
            base = Patch(
                osc1Level = 0.12f, osc1Range = OscRange.SIXTEEN, osc1Wave = OscWave.SAWTOOTH,
                osc2Level = 0f, osc3Level = 0f, noiseLevel = 0.78f,
                cutoff = 0.48f, emphasis = 0.76f, contourAmount = 0.32f, keyTrack = KeyTrack.THIRD,
                filterAttack = 0.05f, filterDecay = 0.35f, filterSustain = 0f,
                ampAttack = 0.02f, ampDecay = 0.5f, ampSustain = 0.6f, ampRelease = 0.3f,
                drive = 0.35f, drift = 0.9f, masterVolume = 0.6f
            )
        ),
        patch(
            name = "Init Patch",
            category = PresetCategory.KEYS,
            description = "A clean single saw at eight foot — your blank canvas.",
            base = Patch(
                osc1Range = OscRange.EIGHT, osc1Wave = OscWave.SAWTOOTH, osc1Level = 0.85f,
                osc2Level = 0f, osc3Level = 0f, noiseLevel = 0f,
                cutoff = 0.58f, emphasis = 0.12f, contourAmount = 0.30f, keyTrack = KeyTrack.TWO_THIRDS,
                filterAttack = 0f, filterDecay = 0.4f, filterSustain = 0.4f,
                ampAttack = 0f, ampDecay = 0.4f, ampSustain = 0.85f, ampRelease = 0.15f,
                drive = 0.1f, glide = 0f, drift = 0.25f
            )
        )
    )

    val byCategory: Map<PresetCategory, List<Preset>> = all.groupBy { it.category }

    const val LEGACY_COUNT = 6
}
