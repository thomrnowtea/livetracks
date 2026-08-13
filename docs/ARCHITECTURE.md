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
  -> ordered playlist: MasterTrack (gain/pan + optional metronome override + tempo-grid visibility + optional click-reference stem)
       -> synchronized Track stems (file, source range, timeline offset, gain/pan, sends)
       -> TimelineMarker lane (type, absolute position, optional voice lead)
```

## Threading

| Context | Responsibility | Communication |
|---|---|---|
| Main thread | Compose and lifecycle | immutable state / Flow |
| ViewModel scope | orchestration | coroutines and repository calls |
| IO dispatcher | versioned project persistence; future decoding | immutable snapshots / future ring buffers |
| Android TTS worker | pre-render section announcements while editing | validated cached WAV loaded before playback |
| Oboe callback | bounded render and frame clock | atomics and preallocated audio buffers |
| Device callback | route observations | callback -> StateFlow; safety stop |

JNI calls only configure/control the engine or read cheap diagnostics. No JNI work occurs from the realtime callback. The output stream remains the only transport clock. Stem entry offsets are converted from the persisted 48 kHz timeline base to actual output frames before playback; the callback only reads those preconfigured atomics.

Extraction is a domain operation over the ordered playlist: it removes one clip from its source master, resets the clip timeline origin, inserts a new master immediately after the source, and copies the effective metronome into an explicit override. It does not rewrite or duplicate the source audio file.
