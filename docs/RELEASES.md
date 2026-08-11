# Release process

## Continuous verification

Every push to `main` or `agent/**` and every pull request runs unit tests, Android lint, the Kotlin build, and both configured native ABIs. The debug APK is retained as a short-lived workflow artifact.

## Create a release

1. Move completed entries from `Unreleased` in `CHANGELOG.md` into a version section.
2. Update the local fallback `versionName` and increment `versionCode` in `app/build.gradle.kts`.
3. Verify locally with `./gradlew testDebugUnitTest lintDebug assembleDebug`.
4. Merge the release change to `main`.
5. Create and push an annotated semantic tag.
6. GitHub Actions builds and verifies the signed APK, publishes its checksum and creates the GitHub Release.

## Signing

Release signing material is maintained privately by the project owner. No keystore, password, encoded key, local secure path, or recovery instruction belongs in this repository.

Stable downloads use `https://github.com/thomrnowtea/livetracks/releases/latest/download/LiveTracks.apk`. Release candidates use suffixed semantic tags and are marked as pre-releases.
