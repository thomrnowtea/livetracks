# Implementation status

Last updated: 2026-08-13

## Done

- Repository/toolchain bootstrap, Compose, NDK/CMake, Oboe Prefab, and documentation.
- Immutable Project/MasterTrack/Track/TimelineMarker/output models, safe click/cue defaults, schema-v6 atomic persistence, and v1-v5 migration tests.
- Native Oboe stream, allocation-free mixing callback, absolute transport-frame counter, JNI diagnostics, Android output enumeration, and route-change emergency stop.
- Four adaptive portrait/landscape workspaces with persistent transport: Projects (List/Mixer), Playlist (List/Mixer), Track (Timeline/Stem Mixer), and Click/Routing.
- Adaptive Settings workspace with persisted Spanish/English selection, screen-awake and confirmation controls, configurable new-stem defaults, version details, and credits.
- Original fader/waveform brand mark, Source Sans 3 typography, and restrained DAW graphite/steel visual system.
- GitHub Actions verification on pushes/PRs and tag-driven, consistently signed APK releases with checksums.
- In-app GitHub Releases updater with automatic/manual checks, stable/pre-release preference, resumable system downloads, SHA-256/package/signing-certificate validation, and explicit Android installer permission/confirmation.
- Project and playlist-item create/rename/delete/reorder, SAF multi-file stem selection, persisted URI access, empty stems with explicit duration, stem removal, and MUSIC/CLICK/CUE classification.
- Native decoding for mono/stereo RIFF WAV in PCM 8/16/24/32-bit or float32. Imported tracks mix from one absolute output-frame position with linear sample-rate adaptation.
- Adaptive console banks for projects, playlist items, and stems. Portrait uses full-height vertical strips with horizontal scrolling; landscape uses full-width horizontal channels stacked vertically. Stem channels retain mute/solo, MAIN/MONITOR sends, and real callback meters.
- Per-stem draggable timeline entry offsets connected to native playback; drag preview is continuous, total duration includes the latest offset plus source duration, and a persistent switch selects zoom-unit snapping or frame-accurate free movement.
- Real peak envelopes for WAV and Android-decoded audio, virtual millisecond zoom, a transport-synchronized draggable playhead, edge/playhead snapping, non-destructive split, and a 50-step Undo/Redo timeline history.
- Musical beat/downbeat grid driven by each master track's effective metronome, per-song visibility, beat/marker snapping, and draggable marker lines spanning every stem lane.
- Timeline/Mix Console selection in the global context header, with independently collapsible tool rail and stem-label panel whose state survives portrait/landscape recomposition.
- Compact non-scrolling edit toolbar with persistent Add/Undo/Redo actions, always-visible zoom/scale controls, and a labeled overflow menu for Snap, markers, extraction, and destructive actions.
- Two-row transport with a dominant Play control above a full-width seek bar; secondary workspace, Stop, and Panic actions live in an explicit overflow menu.
- Unified scrollable Click/Routing workspace for the selected song's metronome, click output, audition routing, and physical output state.
- Safe previous/next show transport: a skip stops output, resets the playhead, and arms the neighboring master track without autoplay.
- Collapsible Playlist preparation header plus a full-screen Stage Mode for performers, with a clean cue list, armed/live state, next-song preview, large Previous/Play/Stop/Next targets, single-tap arming, and double-tap immediate playback. Editable playlist double tap opens the song Timeline.
- Natural completion advances to the following playlist item and continues playback without requiring another Play action; the final item stops without wrapping.
- Terminal waveform states distinguish an intentionally empty region from an unavailable analysis instead of leaving an endless analysis label.
- Reversible extraction of a selected split clip or stem into the next independent master track, with its own copied metronome override.
- Typed section markers with optional pre-rendered Android TTS announcements, configurable lead beats, MONITOR-only routing, solo-safe playback, and persistence/migration coverage.
- Optional exclusive performance mode with normal media audio focus, temporary Do Not Disturb policy, automatic restoration, and a separate keep-screen-awake switch.
- Android MediaCodec preprocessing for device-supported MP3, AAC/M4A, FLAC and OGG sources; conversion occurs before playback and never in the realtime callback.
- Versioned persistent waveform envelopes validated against source fingerprints and preloaded for all project tracks before the first rendered UI state.
- Two-stage master configuration: project volume/pan plus selected playlist-item volume/pan.
- Per-master-track transport-locked metronome with an inheritable project template, adaptive project/custom editing cards, directly writable 20–400 decimal BPM, and fully custom 1–32 / 1–32 meter values. A single persisted stem may replace the native click; designation converts it to CLICK, forces MAIN to negative infinity, and keeps BPM/meter available for the grid, snapping, and cues. There is no global playback metronome.
- Local verification: unit tests passed; `armeabi-v7a`, `arm64-v8a`, and `x86_64` native builds and APK packaging passed; real WAV envelopes and active x86_64 emulator audio output passed in portrait and landscape.
- Release verification: the owner completed the in-app signed upgrade from `v0.1.0-alpha.1` to `v0.2.0-alpha.1` on a physical Android phone. The device model is intentionally not recorded in the public repository because this result validates the updater flow, not audio-hardware compatibility.

## Partial

- WAV files are predecoded into memory before playback: maximum 16 tracks and 512 MB per file. Decoder-worker ring buffers are still required for long show assets.
- `armeabi-v7a` installation is packaged and compile-verified, but physical 32-bit playback and the tighter memory ceiling of typical 32-bit devices remain unvalidated.
- Route-change handling is connected; full foreground playback-service/lifecycle recovery is pending.
- Voice cues depend on an installed Android TTS voice and currently allow up to 32 enabled announcements per master track.

## Pending

- Decoder-worker ring buffers, coordinated seek, and pre-Play file metadata validation.
- Editable MAIN/MONITOR send levels, count-in, and complete Stereo Split voice/click output tests.
- Live Mode, foreground playback service, USB multichannel, and video.

## Blocked by hardware

- Physical Android device latency/stability, USB-C DAC routing, actual multichannel exposure, hub/PD compatibility, and unplug behavior.

## How to test

Install the APK, create/select a project, add a playlist item, open Track, and choose PCM WAV stems. Validate project, playlist, and stem mixers in portrait and landscape. At every zoom level, enable Snap and verify a dragged stem lands on the displayed time unit; disable it and verify a deliberately off-grid offset survives release and relaunch. Relaunch twice and confirm cached waveforms appear without an analysis pass. In editable Playlist, double-tap a cue and confirm its Timeline opens. Enter Stage Mode: single-tap arms, double-tap starts playback, and natural completion continues into the next cue. Exercise Previous/Play/Stop/Next with real audio before returning to editing. Split a stem, extract the second clip, and confirm a neighboring master track is created with an independent metronome. Add and drag section markers; verify beat/downbeat lines, millisecond placement, TTS render state, lead time, and that speech remains absent from MAIN. Open Stem Mixer and press Play; confirm delayed stems, meters/faders/mute/solo, click routing, screen-awake, and exclusive-mode restoration. Physical audio routing and long-file stability remain unverified until results are recorded on the exact stage hardware.
