# Changelog

All notable changes will be documented here. Versions follow Semantic Versioning while LiveTracks remains pre-1.0.

## Unreleased

## [0.1.0-alpha.1] - 2026-08-11

### Added

- Adaptive Projects, Playlist, Timeline, Mix Console, Master, and Settings workspaces.
- Persistent Spanish/English preference, general settings, stem import defaults, and About credits.
- Original LiveTracks fader/waveform brand mark and DAW-oriented typography/color system.
- GitHub CI and tag-driven, consistently signed APK releases with SHA-256 checksums and stable download metadata.
- Real WAV waveform envelopes, millisecond Timeline zoom, a draggable global playhead, clip snapping, and non-destructive stem splitting.
- Original SVG icon source sheet and Android vector exports for navigation, editing, transport, and mixer controls.
- In-app release updater with stable/pre-release channels, resumable downloads, SHA-256/package/signature checks, and explicit Android installation approval.

### Changed

- Increased the application font scale and reduced permanent Timeline actions by moving clip tools into the selection context.
- Expanded the README with the complete project hierarchy, show workflow, audio architecture, safety model, supported formats, and current limits.
- Enlarged iconography and touch targets, and removed duplicated brand and Settings controls from the wide layout.
- Clarified that release-signing material and recovery instructions remain outside the public repository.

### Safety

- Click and cue MAIN sends remain muted by default.
- Physical route changes stop output and require revalidation.
