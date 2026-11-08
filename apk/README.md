# Monarch D — installable APK

`MonarchD-1.0.0.apk` is a signed release build of the app in this repository.

## Install on Android

1. Copy the APK to your phone (or download it from GitHub).
2. Open it and allow installs from this source when Android asks.
3. Launch **Monarch D** and press **ENTER THE PANEL**.

The release APK is signed with the standard Android debug key so that it can be
installed directly while the project is in development. A Play Store release
should be signed with a private upload key (see the signing note in the README).

Minimum Android version: Android 8.0 (API 26).

The same APK is rebuilt by the **Android Build** GitHub Actions workflow on every
push and by the **Publish APK to main** workflow on demand.
