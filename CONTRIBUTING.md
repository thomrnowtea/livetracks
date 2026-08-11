# Contributing to LiveTracks

LiveTracks is stage software. Reliability, deterministic playback, and safe click routing take precedence over feature count.

## Change flow

1. Start from `main` and create `agent/<short-description>` or `feature/<short-description>`.
2. Keep commits focused and update `docs/STATUS.md` when implemented scope changes.
3. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` with JDK 17.
4. Install the APK for UI/audio changes and record any physical-device claim in `docs/HARDWARE_COMPATIBILITY.md`.
5. Open a pull request with the provided safety checklist.

## Releases

Development follows semantic version tags. A tag such as `v0.2.0` runs the release workflow and creates a GitHub Release containing a consistently signed, directly installable APK. Tags containing a suffix such as `v0.2.0-rc1` are marked as pre-releases.

Never commit signing keys, local SDK paths, selected audio files, generated screenshots, or device-specific secrets.
