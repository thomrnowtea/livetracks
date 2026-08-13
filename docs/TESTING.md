# Testing

## Local gates

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew verifyDebugApkAbis
```

Unit tests cover decibel conversion, pan-law endpoints/center, MAIN downmix headroom, click/cue routing defaults, click-reference safety, beat/bar spacing, duration selection, safe metronome defaults, schema migration/rejection through v6, marker/grid/reference persistence, non-destructive splits, independent-master extraction, marker lead timing, real PCM waveform analysis, persistent waveform-cache encoding/rejection, playlist completion policy, and update version-code policy. The ABI gate checks that `liblivetracks_audio.so`, `liboboe.so`, and `libc++_shared.so` are packaged for `armeabi-v7a`, `arm64-v8a`, and `x86_64`. A successful build verifies Kotlin/JNI/CMake linkage; it does not verify audible output.

For metronome editing, type a decimal value such as `137.5` BPM and a non-preset meter such as `7/10`. Confirm the same exact values drive the beat grid, survive relaunch, and remain editable in both project-template and selected-song modes. Values outside BPM 20–400 or meter parts 1–32 must never reach persistence or the native engine.

## Emulator

Use an API 31 or newer emulator for navigation, state restoration, persistence, route-state UI, system-picker imports, and real-WAV smoke tests. Emulator audio does not validate latency, USB routing, multichannel output, drift, or long-run stability.

Run every edited workspace once in portrait and once in landscape with system autorotation enabled; the manifest must not lock either orientation. Confirm project, playlist, and stem mixers use vertical strips with horizontal scrolling in portrait and horizontal strips with vertical scrolling in landscape. For timeline changes, verify beat density at overview and millisecond zoom, marker drag/edit/delete, voice render success/failure state, split then extract, and Undo/Redo across the newly inserted master. Load at least two masters backed by real WAV files: verify visible envelopes and immediate audible output, relaunch twice to confirm no repeated analysis state, and verify previous/next availability at playlist boundaries. Grant and revoke Notification Policy access and confirm exclusive mode never prevents playback, restores the prior policy on Pause/Stop/Panic, and leaves media output audible.

Keep Timeline zoom-out, zoom-in, and the current scale visible in both orientations. With Snap enabled, drag at every zoom step and assert the persisted frame offset is an exact multiple of that displayed unit. Disable Snap, perform a deliberately off-grid drag, confirm the toolbar reads `FREE`/`LIBRE`, and verify the exact offset and preference survive relaunch. A free drag must not show a magnetic guide.

For Stage Mode, verify the Playlist preparation header collapses/restores and retains that state after entering and leaving the full-screen view. In portrait and landscape, confirm every cue remains readable and all four transport targets remain visible. Single tap must only arm; double tap must select and play. Previous/Next stop before arming, Stop resets to zero, natural completion selects and starts the next cue without another Play action, the last cue stops without wrapping, Android Back returns to editing, and no edit/navigation controls leak into the performance view. In editable Playlist, double tap must open that cue's Timeline instead of playing it.

For the signed updater path, start from an older release-signed APK. Test stable and pre-release filtering, interrupted-download recovery, a valid upgrade, and deliberately altered metadata/checksum/package/signature. Debug builds use the separate `com.thomrnowtea.livetracks.debug` application id and certificate so they can coexist with a release installation; they must reject a release APK, and that rejection is expected rather than an updater failure.

## Recorded release checks

- `2026-08-13`: the project owner completed the in-app update from signed `v0.1.0-alpha.1` (version code 1) to signed `v0.2.0-alpha.1` (version code 2) on a physical Android phone.
- Both published APKs were independently checked against their release SHA-256 files and verified as package `com.thomrnowtea.livetracks` with the same signing-certificate digest.
- An emulator package upgrade preserved `firstInstallTime` and a project created under schema v5 was visible after migration to schema v6.

The physical device model is deliberately omitted because this check validates release discovery/download/install and persistence migration only. It must not be cited as evidence for latency, routing, USB or long-session stability.

## Physical sequence

1. Internal speaker: open output, run conservative tone, verify Single Mix and diagnostics.
2. Stereo USB-C DAC: select Stereo Split and identify left/right with output tests.
3. USB interface: compare reported channels with actual opened channels before enabling multichannel.
4. While rendering, unplug USB: output must stop, state must become UNSAFE, and no click may continue on speaker.
5. Run 5, 30, and 60-minute WAV sessions at 2/4/8 stems; record xruns, memory, temperature, and failures. Extend to 16 only if stable.
6. On a 32-bit ARM target, confirm `armeabi-v7a` appears in `ro.product.cpu.abilist`, begin with one short WAV, and record process-memory pressure before increasing stem count or duration.

Useful collection commands:

```text
adb shell getprop ro.build.version.release
adb shell getprop ro.product.cpu.abilist
adb shell dumpsys media.audio_flinger
adb logcat --pid=$(adb shell pidof com.thomrnowtea.livetracks)
adb shell dumpsys package com.thomrnowtea.livetracks
```
