# Testing

## Local gates

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Unit tests cover decibel conversion, pan-law endpoints/center, MAIN downmix headroom, click/cue routing defaults, click-reference safety, beat/bar spacing, duration selection, safe metronome defaults, schema migration/rejection through v6, marker/grid/reference persistence, non-destructive splits, independent-master extraction, marker lead timing, real PCM waveform analysis, and update version-code policy. A successful build verifies Kotlin/JNI/CMake linkage; it does not verify audible output.

## Emulator

Use an API 31 or newer emulator for navigation, state restoration, persistence, route-state UI, system-picker imports, and real-WAV smoke tests. Emulator audio does not validate latency, USB routing, multichannel output, drift, or long-run stability.

Run every edited workspace once in portrait and once in landscape with system autorotation enabled; the manifest must not lock either orientation. For timeline changes, verify beat density at overview and millisecond zoom, marker drag/edit/delete, voice render success/failure state, split then extract, and Undo/Redo across the newly inserted master. Load at least two masters backed by real WAV files: verify visible envelopes, immediate audible output, previous/next availability at playlist boundaries, and that a skip stops and arms without autoplay before the second master is played. Grant and revoke Notification Policy access and confirm exclusive mode never prevents playback, restores the prior policy on Pause/Stop/Panic, and leaves media output audible.

For Stage Mode, verify the Playlist preparation header collapses/restores and retains that state after entering and leaving the full-screen view. In portrait and landscape, confirm every cue remains readable, all four transport targets remain visible, selecting a row only arms it, Previous/Next stop before arming, Stop resets to zero, Play is explicit, Android Back returns to editing, and no edit/navigation controls leak into the performance view.

For the signed updater path, start from an older release-signed APK. Test stable and pre-release filtering, interrupted-download recovery, a valid upgrade, and deliberately altered metadata/checksum/package/signature. A debug APK has a different certificate and must reject a release APK; that rejection is expected, not an updater failure.

## Physical sequence

1. Internal speaker: open output, run conservative tone, verify Single Mix and diagnostics.
2. Stereo USB-C DAC: select Stereo Split and identify left/right with output tests.
3. USB interface: compare reported channels with actual opened channels before enabling multichannel.
4. While rendering, unplug USB: output must stop, state must become UNSAFE, and no click may continue on speaker.
5. Run 5, 30, and 60-minute WAV sessions at 2/4/8 stems; record xruns, memory, temperature, and failures. Extend to 16 only if stable.

Useful collection commands:

```text
adb shell getprop ro.build.version.release
adb shell dumpsys media.audio_flinger
adb logcat --pid=$(adb shell pidof com.thomrnowtea.livetracks)
adb shell dumpsys package com.thomrnowtea.livetracks
```
