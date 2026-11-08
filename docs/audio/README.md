# Audio demos

These three clips were rendered offline from the same `SynthVoice` used by the
Android audio thread. They contain no samples and were not recorded through a
microphone or speaker.

| Clip | Patch and performance | Length |
| --- | --- | --- |
| [`01-fat-bass-filter-sweep.wav`](01-fat-bass-filter-sweep.wav) | *Model One* bass line with a slow ladder sweep | 5.0 s |
| [`02-glide-lead-mod-wheel.wav`](02-glide-lead-mod-wheel.wav) | *Whistle King* legato lead with portamento and a rising mod wheel | 4.2 s |
| [`03-resonant-ladder-self-oscillation.wav`](03-resonant-ladder-self-oscillation.wav) | *Acid Reign* with high emphasis and an opening filter | 4.3 s |

All three files are 48 kHz, 16-bit mono, peak-normalised to −1 dBFS. The demo
render uses the same oscillator, ladder, envelope and drive code as the app;
`AudioTestSupport.kt` supplies the offline renderer and the WAV writer used to
produce these files. The clips are intentionally short so they are quick to
audition from the repository.

The automated `AudioEngineTest` suite is the primary audio-quality check. It
renders the voice at 48 kHz and measures pitch, harmonic content, filter
response, contour timing, drift, detune, note priority, glide, transients, DC
offset and headroom. It is not a golden-file comparison: the assertions test
the musical properties of the rendered signal. The representative results are
listed in the main [README](../../README.md#offline-audio-results).
