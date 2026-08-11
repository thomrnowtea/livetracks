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

Each release publishes three assets: `LiveTracks.apk`, `LiveTracks.apk.sha256`, and schema-v1 `release.json`. The metadata contains the exact tag download URL, Android version code, package name, and APK SHA-256 used by the in-app updater. A pre-release must use its tag-specific URL because GitHub's `latest` endpoint only represents the latest stable release.

## Signing

Release signing material is maintained privately by the project owner. No keystore, password, encoded key, local secure path, or recovery instruction belongs in this repository.

Stable manual downloads may use `https://github.com/thomrnowtea/livetracks/releases/latest/download/LiveTracks.apk`. Every updater download uses the immutable tag-specific URL from `release.json`. Release candidates use suffixed semantic tags, are marked as pre-releases, and are only offered when the user enables that channel.

## Updater safety contract

The app trusts only HTTPS GitHub release hosts for this repository. Before opening Android's package installer it verifies the metadata schema, tag, version code, package id, APK SHA-256, archive version, and signing certificate against the installed app. Download state can resume after process recreation. Android's per-app unknown-source permission and final installer confirmation remain mandatory; LiveTracks never attempts a silent install.
