# Monarch D — installable APK

`MonarchD-1.0.1.apk` is a signed release build of the app in this repository.

## Install on Android

1. Copy the APK to your phone (or download it from GitHub).
2. Open it and allow installs from this source when Android asks.
3. Launch **Monarch D** and press **ENTER THE PANEL**.

The release APK is signed with the standard Android debug key so that it can be
installed directly while the project is in development. A Play Store release
should be signed with a private upload key (see the signing note in the README).

Minimum Android version: Android 8.0 (API 26).

The **Android Build** GitHub Actions workflow tests and builds this APK on every
push to `main`, then publishes the exact build artifact and its SHA-256 checksum
back to this directory. The `Publish APK to main` workflow is a manual fallback.
The current file is therefore traceable to a green CI run, not a hand-built
binary. Its checksum is in `MonarchD-1.0.1.apk.sha256`.
