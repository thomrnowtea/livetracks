# Audio engine

The native engine opens one float stereo Oboe output stream. Before playback, an IO worker calls the native RIFF decoder for each SAF file descriptor. Mono/stereo PCM 8/16/24/32-bit and float32 WAV are converted to float32. The callback performs no file I/O or allocation and advances one absolute transport-frame counter only while playing.

Each source position is derived from the same output frame; differing source rates use linear interpolation against that position. Per-clip source ranges make Timeline splits non-destructive while preserving the same master clock. Console faders, pan, mute, solo, and MAIN/MONITOR sends feed atomics consumed by the callback. Click and cue track factories set MAIN to negative infinity. Stereo Split renders MAIN mono on physical left and MONITOR on physical right.

The engine converts the longest loaded source length into output-stream frames and exposes it with the absolute transport position. Seek writes one clamped output-frame position, so every stem resumes from the same timeline point. Playback stops at the real project end; pressing Play from the end resets to frame zero.

The allocation-free callback also renders a configurable metronome from the same transport clock. BPM, numerator, denominator, level, enabled state, and explicit MAIN audition are atomic controls. Click always feeds MONITOR when enabled; MAIN audition defaults off. In Stereo Split this keeps music/FOH on physical left and click/monitor on physical right.

The Oboe stream is opened for preflight but started only with Play and explicitly paused with Pause/Stop. Natural end is detected in the callback, then the UI worker pauses the stream outside realtime context. This prevents an idle emulator or device from accumulating output underruns while the transport is stopped.

The current decoder preloads each WAV, capped at 512 MB, so this is functional synchronized WAV playback but not the final long-show memory architecture. The next engine milestone replaces preloading with decoder workers and single-producer/single-consumer ring buffers while preserving the callback contract. The realtime callback remains limited to bounded DSP, preallocated buffers, atomics, and counters; it must not allocate, log, decode, access storage, or wait.
