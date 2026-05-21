# :sample — Lingyun SDK demo app

Exercises the `:sdk-media` library. Phase 1 demonstrates audio waveform
visualization: hold to record, tap to play back, watch the bezier-circle
waveform animate around the play button.

## Run

```
./gradlew :sample:installDebug
```

Open "Lingyun SDK Demo" on the device. Tap "Audio waveform demo".

## Phase 1 scope

- ✅ Audio waveform demo (this phase)
- 🚧 Lottie demo (Phase 2 — placeholder button)
- 🚧 Photo viewer demo (Phase 3 — placeholder button)

## Architecture

- `WaveformBuffer` (pure Kotlin, unit-tested) — converts a stream of
  amplitude samples (0..1) into the `FloatArray(8)` shape that
  `AudioVisualizerDrawable.setWaveform` expects.
- `AudioRecorderHelper` — wraps Android `MediaRecorder` (record) and
  `MediaPlayer` (playback). Polls `getMaxAmplitude` at 50ms cadence during
  recording.
- `WaveformView` — a custom `View` that owns a single
  `AudioVisualizerDrawable` instance, exposes `pushAmplitude` /
  `setPlaying`, and draws the drawable centered.
- `AudioWaveformActivity` — ties it all together; handles the
  `RECORD_AUDIO` permission flow.

No Telegram backend (TLRPC, FileLoader, MessagesController) is used.
