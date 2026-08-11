# Implementation status

Last updated: 2026-08-11

## Done

- Repository/toolchain bootstrap, Compose, NDK/CMake, Oboe Prefab, and documentation.
- Immutable Project/MasterTrack/Track/output models, safe click/cue defaults, schema-v3 atomic persistence, and v1/v2 migration tests.
- Native Oboe stream, allocation-free mixing callback, absolute transport-frame counter, JNI diagnostics, Android output enumeration, and route-change emergency stop.
- Four adaptive portrait/landscape workspaces with persistent transport: Projects, Playlist, Track (Timeline/Mix Console), and Master.
- Adaptive Settings workspace with persisted Spanish/English selection, screen-awake and confirmation controls, configurable new-stem defaults, version details, and credits.
- Original fader/waveform brand mark, Source Sans 3 typography, and restrained DAW graphite/steel visual system.
- GitHub Actions verification on pushes/PRs and tag-driven, consistently signed APK releases with checksums.
- In-app GitHub Releases updater with automatic/manual checks, stable/pre-release preference, resumable system downloads, SHA-256/package/signing-certificate validation, and explicit Android installer permission/confirmation.
- Project and playlist-item create/rename/delete/reorder, SAF multi-file stem selection, persisted URI access, empty stems with explicit duration, stem removal, and MUSIC/CLICK/CUE classification.
- Native decoding for mono/stereo RIFF WAV in PCM 8/16/24/32-bit or float32. Imported tracks mix from one absolute output-frame position with linear sample-rate adaptation.
- Horizontally scrollable channel bank with large console-style vertical faders, rotary pan knobs, mute/solo, MAIN/MONITOR sends, and real callback meters. Landscape exposes about three channels; portrait exposes one to two without shrinking controls.
- Per-stem draggable timeline entry offsets connected to native playback; total duration includes the latest offset plus source duration.
- Real peak envelopes for WAV and Android-decoded audio, virtual millisecond zoom, a transport-synchronized draggable playhead, edge/playhead snapping, non-destructive split, and a 50-step Undo/Redo timeline history.
- Android MediaCodec preprocessing for device-supported MP3, AAC/M4A, FLAC and OGG sources; conversion occurs before playback and never in the realtime callback.
- Two-stage master configuration: project volume/pan plus selected playlist-item volume/pan.
- Per-master-track transport-locked metronome with an inheritable project template. There is no global playback metronome; MAIN send remains protected by default.
- Local verification: unit tests passed; arm64-v8a and x86_64 native builds passed; app installed on the landscape emulator.

## Partial

- WAV files are predecoded into memory before playback: maximum 16 tracks and 512 MB per file. Decoder-worker ring buffers are still required for long show assets.
- Route-change handling is connected; full foreground playback-service/lifecycle recovery is pending.

## Pending

- Decoder-worker ring buffers, coordinated seek, waveform cache, and pre-Play file metadata validation.
- Editable MAIN/MONITOR send levels, count-in/cues timeline, playlist auto-advance, and complete Stereo Split output tests.
- Live Mode, foreground playback service, USB multichannel, and video.

## Blocked by hardware

- Physical Android device latency/stability, USB-C DAC routing, actual multichannel exposure, hub/PD compatibility, and unplug behavior.

## How to test

Install the APK, create/select a project, add a playlist item, open Track, and choose PCM WAV stems. Drag a region in Timeline and confirm the total duration changes. Open Mix Console and press Play; confirm the delayed stem enters at its marker, meters/faders/mute/solo affect real audio, and click remains absent from MAIN. Configure project defaults and per-item overrides only from Master. In Settings > About, verify both update channels, download an official newer signed build, reject an altered checksum/package/signature, authorize LiveTracks as an install source, and confirm Android still requires installation approval. Physical routing, long-file stability, and the signed updater path remain unverified until results are recorded on physical hardware.
