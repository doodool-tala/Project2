# Render matrix

These are raw screenshots of the shipping Compose hierarchy, captured by the
JVM/Robolectric tests in `app/src/test/java/com/monarchd/synth/ui`. They are not
mockups and do not require an emulator or a physical device.

The renderer draws the activity window and then composites any shown
`ModalBottomSheet` dialog windows into the same bitmap, so Settings, Help and the
Patch Library include the sheet and its scrim rather than only the instrument
behind them.

| Device class | Density qualifier | Pixels |
| --- | --- | --- |
| Phone portrait | `w390dp-h844dp-port-xhdpi` | 780 × 1688 |
| Phone landscape | `w844dp-h390dp-land-xhdpi` | 1688 × 780 |
| Tablet | `w1280dp-h800dp-land-xhdpi` | 2560 × 1600 |

## Panel pages

| Page | Phone portrait | Phone landscape | Tablet |
| --- | --- | --- | --- |
| OSC | [PNG](phone-portrait-panel-01-osc.png) | [PNG](phone-landscape-panel-01-osc.png) | [PNG](tablet-panel-01-osc.png) |
| MIX | [PNG](phone-portrait-panel-02-mix.png) | [PNG](phone-landscape-panel-02-mix.png) | [PNG](tablet-panel-02-mix.png) |
| FILTER | [PNG](phone-portrait-panel-03-filter.png) | [PNG](phone-landscape-panel-03-filter.png) | [PNG](tablet-panel-03-filter.png) |
| ENV | [PNG](phone-portrait-panel-04-envelopes.png) | [PNG](phone-landscape-panel-04-envelopes.png) | [PNG](tablet-panel-04-envelopes.png) |
| PERF | [PNG](phone-portrait-panel-05-performance.png) | [PNG](phone-landscape-panel-05-performance.png) | [PNG](tablet-panel-05-performance.png) |

## Sheets and onboarding

| Surface | Phone portrait | Phone landscape | Tablet |
| --- | --- | --- | --- |
| Settings | [PNG](phone-portrait-settings.png) | [PNG](phone-landscape-settings.png) | [PNG](tablet-settings.png) |
| Settings · About | [PNG](phone-portrait-settings-about.png) | [PNG](phone-landscape-settings-about.png) | [PNG](tablet-settings-about.png) |
| How to play | [PNG](phone-portrait-help.png) | [PNG](phone-landscape-help.png) | [PNG](tablet-help.png) |
| Patch library | [PNG](phone-portrait-library.png) | [PNG](phone-landscape-library.png) | [PNG](tablet-library.png) |
| Intro | [PNG](phone-portrait-intro.png) | [PNG](phone-landscape-intro.png) | [PNG](tablet-intro.png) |

## Reproduce

From the repository root:

```bash
./gradlew --no-daemon testDebugUnitTest \
  --tests 'com.monarchd.synth.ui.PhonePortraitScreenshotTest' \
  --tests 'com.monarchd.synth.ui.PhoneLandscapeScreenshotTest' \
  --tests 'com.monarchd.synth.ui.TabletScreenshotTest'
```

The PNGs are written to `app/build/screenshots/`. The images in this directory
are the audited copies committed with the release source.
