# Architecture

```text
Compose UI
  -> ViewModel / immutable UI state
    -> domain use cases
      -> ProjectRepository     -> versioned atomic local files
      -> PlaybackController    -> JNI bridge
                                  -> C++ AudioEngine
                                     -> one Oboe output callback
```

The application uses one Gradle app module. Package boundaries preserve the intended layering without paying multi-module build cost before multipista audio is proven.

## Domain hierarchy

```text
Project (master gain/pan + metronome template)
  -> ordered playlist: MasterTrack (gain/pan + optional metronome override)
       -> synchronized Track stems (file, source range, timeline offset, gain/pan, sends)
```

## Threading

| Context | Responsibility | Communication |
|---|---|---|
| Main thread | Compose and lifecycle | immutable state / Flow |
| ViewModel scope | orchestration | coroutines and repository calls |
| IO dispatcher | versioned project persistence; future decoding | immutable snapshots / future ring buffers |
| Oboe callback | bounded render and frame clock | atomics and preallocated audio buffers |
| Device callback | route observations | callback -> StateFlow; safety stop |

JNI calls only configure/control the engine or read cheap diagnostics. No JNI work occurs from the realtime callback. The output stream remains the only transport clock. Stem entry offsets are converted from the persisted 48 kHz timeline base to actual output frames before playback; the callback only reads those preconfigured atomics.
