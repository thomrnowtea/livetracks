## Summary

- What changed:
- Why:

## Validation

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew lintDebug`
- [ ] `./gradlew assembleDebug`
- [ ] Installed on an emulator or device when UI/audio changed

## Realtime safety

- [ ] No allocation, logging, storage, JNI, waiting, or contended lock was added to the Oboe callback
- [ ] Click MAIN remains muted by default
- [ ] Route changes still stop/mute playback and require revalidation

