# Changelog

All notable changes will be documented here. Versions follow Semantic Versioning while LiveTracks remains pre-1.0.

## Unreleased

### Changed

- Updated CI and release actions to their current Node 24-compatible major versions and added scheduled dependency update checks with explicit toolchain-compatibility limits.

## [0.2.0-alpha.1] - 2026-08-13

### Added

- Musical beat/downbeat grid and beat-aware clip snapping for every master timeline.
- Typed, draggable section markers with optional pre-rendered voice cues and configurable lead beats.
- Clip extraction into a new independent master track with its own metronome and full Undo/Redo support.
- General Settings switches for keep-screen-awake and exclusive performance mode using audio focus and temporary Do Not Disturb access.
- Safe SVG previous/next transport controls that stop and arm the neighboring playlist item without autoplay.
- Full-screen Stage Mode for drummer-operated playback with a clean cue list, armed/live feedback, next-song preview, and extra-large Previous/Play/Stop/Next controls.
- Per-song tempo-grid visibility and a persistent, single click-reference stem that replaces the native click while remaining MONITOR-only.

### Changed

- Expanded the native source bank to reserve 32 solo-safe, MONITOR-only voice-cue slots after the 16 user stems.
- Removed the duplicate project creation action from the empty Projects state.
- Moved Timeline/Mix Console selection into the global context header and removed the redundant track header row.
- Kept the Timeline tool rail and stem-label panel independently collapsible, with the chevron anchored consistently on the right.
- Replaced the endless waveform-analysis placeholder with explicit empty-region and unavailable-waveform terminal states.
- Replaced text actions such as GO, Rename, Open, and Master with original SVG controls and accessible descriptions.
- Rebuilt the Timeline toolbar as four persistent editing actions plus a contextual overflow, eliminating horizontal button scrolling and inline zoom-scale text.
- Rebuilt transport around a large circular Play control, a full-width progress row, and a secondary action menu.
- Reorganized Master into show output, song output, click/tempo, and routing scopes with icon navigation and adaptive summaries.
- Increased the global minimum font scale, removed labels from persistent navigation rails, reduced decorative button chrome, and standardized margins on an 8/4 dp grid.
- Made the Playlist preparation header collapsible and moved rename, Master, and delete into its overflow so Stage Mode and Add remain primary.
- Clarified the musical grid hierarchy: amber downbeats, high-contrast beat lines, subdued time subdivisions, and a separate amber playhead.
- Rebuilt the marker editor with adaptive section chips and a compact voice-cue control.
- Audited the live Español/English switch, including persisted language selection and previously untranslated transport/Master states.
- Rebuilt the public repository landing page with real app screenshots, a concise product hierarchy, installation and safety guidance, navigable technical documentation, and responsible disclosure instructions.

### Safety

- Voice synthesis and validation occur only while editing; no TTS, file access, allocation, or JNI was added to the realtime callback.
- Exclusive mode restores the previous interruption policy on Pause, Stop, Panic, route invalidation, and normal controller shutdown.
- A click-reference stem is converted to CLICK, forced to negative-infinity MAIN send, and disables the native click generator until the reference is removed.

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
