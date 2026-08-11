# Testing

## Local gates

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Unit tests cover decibel conversion, pan-law endpoints/center, MAIN downmix headroom, click/cue routing defaults, duration selection, safe metronome defaults, schema migration/rejection, versioned persistence, non-destructive splits, real PCM waveform analysis, and update version-code policy. A successful build verifies Kotlin/JNI/CMake linkage; it does not verify audible output.

## Emulator

Use an API 31 or newer emulator for navigation, state restoration, persistence, route-state UI, system-picker imports, and real-WAV smoke tests. Emulator audio does not validate latency, USB routing, multichannel output, drift, or long-run stability.

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
