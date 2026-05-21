# Design: `:sdk-media` Phase 1 — Audio waveform sample

Date: 2026-05-21
Status: Approved (brainstorming phase)
Author: luolingfeng

## Goal

Build the foundational pieces of `:sdk-media` (a new Android library module) and a `:sample` Android application that exercises it. The first concrete milestone is an **audio waveform demo**: the user holds a button to record a short voice clip, then plays it back, while a circular bezier-curve waveform animates around the play button. The waveform rendering reuses Telegram's existing `AudioVisualizerDrawable` — copied verbatim, no API changes.

This is **Phase 1** of three phases. Phase 2 adds Lottie animation rendering; Phase 3 adds PhotoViewer image/video viewing. Each phase has its own spec; this document covers Phase 1 only.

## Why this shape

The project is a long-lived fork of `DrKLO/Telegram`. The decision was made (after one false-started attempt) to treat `TMessagesProj/` as a read-only source-code reference: never modify it, never wire it together with the SDK, never depend on it from outside. The SDK side **copies** code that's useful, with the smallest possible stub layer to keep the original `org.telegram.*` package names working without dragging in the entire Telegram client.

Specifically: `AudioVisualizerDrawable` references `AndroidUtilities`, `LiteMode`, `SharedConfig`, and `Theme`. Each of those, if copied wholesale, transitively pulls in `MessagesController`, `TLRPC`, `FileLoader`, the entire `messenger/` package, and ultimately `librtmessages.so`. To avoid that explosion, we write **minimal stubs** with the same fully-qualified class names and matching method signatures — just enough surface to make `AudioVisualizerDrawable` link and run.

## Module topology

```
Telegram/  (existing git repo, fork of DrKLO/Telegram)
├── TMessagesProj/                     <- upstream code, never modified
├── sdk-media/                          <- NEW Android library module
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   ├── proguard-rules.pro
│   ├── .gitignore
│   └── src/main/
│       ├── AndroidManifest.xml         <- empty manifest
│       └── java/
│           ├── org/telegram/messenger/ <- STUBS (we write these)
│           │   ├── AndroidUtilities.java
│           │   ├── LiteMode.java
│           │   └── SharedConfig.java
│           ├── org/telegram/ui/ActionBar/
│           │   └── Theme.java          <- STUB
│           └── org/telegram/ui/Components/
│               ├── AudioVisualizerDrawable.java  <- verbatim copy
│               └── CircleBezierDrawable.java     <- verbatim copy
│
├── sample/                             <- NEW Android application module
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lingyun/app/sample/
│       │   ├── MainActivity.kt
│       │   └── audio/
│       │       ├── AudioWaveformActivity.kt
│       │       ├── AudioRecorderHelper.kt
│       │       └── WaveformView.kt
│       └── res/
│           ├── layout/activity_main.xml
│           ├── layout/activity_audio_waveform.xml
│           └── values/strings.xml
│
└── settings.gradle                     <- add 2 lines for new modules
```

Dependency rules:
- `:sdk-media` does NOT depend on `:TMessagesProj`.
- `:TMessagesProj` does NOT depend on `:sdk-media` (it already doesn't).
- `:sample` depends on `:sdk-media` only.
- Deleting `:sdk-media` and `:sample` from `settings.gradle` must leave `:TMessagesProj` building exactly as before.

## Copy + stub strategy

### Verbatim copy (2 files, no modifications)

- `org.telegram.ui.Components.AudioVisualizerDrawable` (234 lines) — the circular animated waveform.
- `org.telegram.ui.Components.CircleBezierDrawable` (107 lines) — its bezier-circle helper.

Both files are copied with byte-for-byte content and original `package` declaration. Their imports of `AndroidUtilities` / `LiteMode` / `SharedConfig` / `Theme` resolve to the stubs in `:sdk-media`, not to TMessagesProj's full implementations.

### Stubs (4 files, written by us; minimal API surface)

**`org.telegram.messenger.AndroidUtilities`** — provides exactly the static members `AudioVisualizerDrawable` and `CircleBezierDrawable` consume:

```java
package org.telegram.messenger;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

public final class AndroidUtilities {
    private AndroidUtilities() {}

    public static float density = 1f;
    public static float screenRefreshRate = 60f;
    public static final RectF rectTmp = new RectF();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static int dp(float value) {
        if (value == 0) return 0;
        return (int) Math.ceil(density * value);
    }

    public static void runOnUIThread(Runnable r) { MAIN.post(r); }
    public static void runOnUIThread(Runnable r, long delayMs) { MAIN.postDelayed(r, delayMs); }

    public static void recycleBitmaps(List<Bitmap> bitmaps) {
        if (bitmaps == null) return;
        for (Bitmap b : bitmaps) {
            if (b != null && !b.isRecycled()) {
                try { b.recycle(); } catch (Exception ignored) {}
            }
        }
    }
}
```

The host (here: the sample app) is responsible for keeping `density` and `screenRefreshRate` up to date — both have safe defaults so a sample that forgets to set them still renders correctly.

**`org.telegram.messenger.LiteMode`** — gating the visualizer:

```java
package org.telegram.messenger;

public final class LiteMode {
    private LiteMode() {}
    public static final int FLAG_CHAT_BACKGROUND = 0;   // value irrelevant for stub
    public static boolean isEnabled(int flag) { return true; }
}
```

Default returns `true` so the visualizer always animates. The sample app can later add `setEnabled(int, boolean)` API if it wants opt-out behavior, but Phase 1 doesn't need it.

**`org.telegram.messenger.SharedConfig`** — placeholder. `AudioVisualizerDrawable` imports it but doesn't actually use any field. We ship an empty class for source compatibility:

```java
package org.telegram.messenger;

public final class SharedConfig {
    private SharedConfig() {}
    // Intentionally empty — AudioVisualizerDrawable imports but does not reference.
}
```

**`org.telegram.ui.ActionBar.Theme`** — color access with sensible defaults:

```java
package org.telegram.ui.ActionBar;

public final class Theme {
    private Theme() {}

    // Color key constants used by AudioVisualizerDrawable
    public static final int key_chat_outLoader = 1;
    public static final int key_chat_inLoader = 2;

    public interface ResourcesProvider {
        int getColor(int key);
    }

    public static int getColor(int key) {
        // Default palette: blue tones, since the visualizer is the only consumer in Phase 1
        switch (key) {
            case key_chat_outLoader: return 0xFF2196F3;   // Material Blue 500
            case key_chat_inLoader:  return 0xFF1976D2;   // Material Blue 700
            default:                 return 0xFF888888;   // medium gray fallback
        }
    }

    public static int getColor(int key, ResourcesProvider provider) {
        if (provider != null) return provider.getColor(key);
        return getColor(key);
    }
}
```

`Color int` values are arbitrary defaults — the sample passes `null` for `ResourcesProvider`, so these are what get used. If the sample wants different colors, it implements `ResourcesProvider` and passes it to `AudioVisualizerDrawable.draw(...)`.

## Sample app

### MainActivity

A single landing screen with three buttons (only the first is functional in Phase 1):

```
┌─────────────────────────────────┐
│   Lingyun SDK Demo              │
│                                 │
│   ╭───────────────────────╮     │
│   │ Audio waveform demo   │     │  <- launches AudioWaveformActivity
│   ╰───────────────────────╯     │
│                                 │
│   ╭───────────────────────╮     │
│   │ Lottie demo (Phase 2) │     │  <- disabled / Toast "coming soon"
│   ╰───────────────────────╯     │
│                                 │
│   ╭───────────────────────╮     │
│   │ Photo demo (Phase 3)  │     │  <- disabled
│   ╰───────────────────────╯     │
│                                 │
└─────────────────────────────────┘
```

### AudioWaveformActivity

States: `IDLE` → `RECORDING` → `RECORDED` → `PLAYING` → `RECORDED` (loop).

Layout: a single circular button (record/play toggle) overlaid with `WaveformView`. A small text label below shows the current duration ("0:03" while recording, "0:03 / 0:05" while playing).

```
┌─────────────────────────────────┐
│                                 │
│        ┌───────────┐            │
│        │           │            │
│   ┌────┤    ●      ├────┐       │  <- WaveformView (circular bezier waveform
│   │    │  (button) │    │       │     animates around the button when recording
│   │    │           │    │       │     or playing)
│   │    └───────────┘    │       │
│   └─────────────────────┘       │
│                                 │
│            0:03                 │
│                                 │
└─────────────────────────────────┘
```

On entry: request `RECORD_AUDIO` permission via `ActivityCompat.requestPermissions`. Show a Material `Snackbar` with a "Retry" action if the user denies.

### AudioRecorderHelper

Wraps Android's `MediaRecorder` (record path) and `MediaPlayer` (playback path). Both write/read a single file at `getCacheDir() + "/audio-waveform-sample.m4a"`. Provides a callback `WaveformListener` that gets called every 50 ms with a `FloatArray` of 8 values (the last 7 amplitudes + a "playing" flag in slot 7), exactly the shape `AudioVisualizerDrawable.setWaveform(...)` expects.

The 50 ms sampling cadence comes from the visualizer's `ANIMATION_DURATION = 120` constant — sampling roughly twice per animation frame keeps the visualization responsive without flooding the UI thread.

### WaveformView

Custom `View` that:

1. Owns one `AudioVisualizerDrawable` instance.
2. On construction, calls `viz.setParentView(this)` so the drawable can post invalidations.
3. Exposes one method: `fun setWaveform(amplitudes: FloatArray, playing: Boolean) { viz.setWaveform(playing, true, amplitudes); invalidate() }`
4. In `onDraw(canvas)`, calls `viz.draw(canvas, cx, cy, color, alpha, null)` where `cx`/`cy` are the view center, `color` is `Color.parseColor("#2196F3")` (Material Blue), `alpha` is `1f`.

That's the entire SDK consumption surface for Phase 1.

## Build setup

| Module | Plugin | namespace | minSdk | compileSdk |
|---|---|---|---|---|
| `:sdk-media` | `com.android.library` | `com.lingyun.app.media` | 21 | 35 |
| `:sample` | `com.android.application` | `com.lingyun.app.sample` | 24 | 35 |
| `:TMessagesProj` | (unchanged) | (unchanged) | 21 | 35 |

Versions:
- Android Gradle Plugin 8.6.1
- Kotlin 1.9.20
- Gradle 8.7
- Java 11 source/target

`:sample` minSdk = 24 to use Material3 / Material Components 1.12.0 without compat shims. `:sdk-media` minSdk = 21 so a future consumer with older minSdk can still depend on it.

`:sample` dependencies (Phase 1):
- `androidx.appcompat:appcompat:1.7.0`
- `androidx.core:core-ktx:1.13.1`
- `com.google.android.material:material:1.12.0`
- `project(":sdk-media")`

`:sdk-media` dependencies (Phase 1): only `testImplementation("junit:junit:4.13.2")`. No production-time AndroidX, no `appcompat`, no `material` — the SDK ships pure Android-framework code only.

### settings.gradle delta

Append (do not modify any existing `include` line):

```groovy
include ':sdk-media'
include ':sample'
```

### Root build.gradle

No changes needed. The existing buildscript classpath already has AGP 8.6.1 and Kotlin 1.9.20.

### AndroidManifest

`:sample` declares `android.permission.RECORD_AUDIO`. `MainActivity` is the launcher. `AudioWaveformActivity` is declared `android:exported="false"`.

## Definition of done

Phase 1 is complete when **all** of the following hold:

1. **Compile**
   - `./gradlew :sdk-media:assembleDebug` — BUILD SUCCESSFUL.
   - `./gradlew :sample:assembleDebug` — BUILD SUCCESSFUL, APK at `sample/build/outputs/apk/debug/sample-debug.apk`.
   - `./gradlew :TMessagesProj:assembleDebug` — BUILD SUCCESSFUL, unchanged from `git checkout origin/master` baseline.
2. **Boundaries**
   - `grep -rE "TMessagesProj" sdk-media/ sample/` returns no results.
   - `grep -rE "(sdk-media|com\.lingyun\.app)" TMessagesProj/` returns no results.
3. **Device smoke test** (manual, run from `:sample:installDebug` to an emulator or device)
   - App launches; MainActivity visible with three buttons.
   - "Audio waveform demo" button works; pressing it opens AudioWaveformActivity.
   - First entry prompts for `RECORD_AUDIO`; grant.
   - Press-and-hold record button → circular waveform animates with blue strokes; releases stop the animation and reveal a Play button.
   - Press Play → recorded clip plays back through the device speaker; waveform animates again.
   - Repeated record/play cycles work.
   - Lottie / Photo buttons present but disabled (a Toast saying "Coming in Phase 2 / 3" is acceptable).

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| `AudioVisualizerDrawable` uses a private field or method we didn't anticipate from one of the stubs. | Compile-time discovery: missing symbol → add it to the stub. The stubs are small enough that the iteration is fast. |
| Waveform amplitude sampling doesn't look like Telegram's. | Phase 1 acceptance is "the visualization animates and looks reasonable," not "pixel-perfect match." If the look is off, tune `AudioRecorderHelper`'s amplitude normalization. |
| Theme colors look bad against the sample app's Material theme. | Defaults are documented and easily changed in `Theme.java` stub. The sample can also pass a `ResourcesProvider` if it wants to control colors. |
| MediaRecorder amplitude polling races with the UI thread. | `getMaxAmplitude()` is documented safe to call from any thread. The sample uses a dedicated `Handler.postDelayed` loop, not a raw `Thread`. |
| The 8 `setWaveform` slots don't map cleanly to MediaRecorder output. | The original Telegram code uses `waveform[0..5]` as 6 frequency-band amplitudes and `waveform[6]` as the overall amplitude. The sample uses the same convention; bands 0..5 can be the most recent 6 samples (a simple ring buffer), and band 6 is the current value. |

## Out of scope for Phase 1

- Lottie animation rendering (Phase 2).
- PhotoViewer image/video viewing (Phase 3).
- Native libraries (`librtmessages.so`, `libffmpeg.so`, etc.) — no native code in Phase 1 at all.
- Maven/AAR publishing — SDK is consumed as a Gradle project reference only.
- Instrumentation tests — manual smoke test is the acceptance gate. Unit tests for `AudioRecorderHelper` are nice-to-have but not required.
- Theme key registration policy, host-injected adapters, `MediaSdk.init(...)` bootstrap — all the abstractions from the previous (now-reverted) attempt are explicitly out. Sample app talks to `AudioVisualizerDrawable` directly through the original Telegram API.

## Phases 2 and 3 outline

(For context only — they get their own specs when their turn comes.)

**Phase 2 — Lottie demo.** Copy `RLottieDrawable` + `BitmapsCache` + `DispatchQueue` + minimum dependencies, plus the pre-built `librtmessages.so` from TMessagesProj's `build/intermediates/` directory into `:sdk-media/src/main/jniLibs/`. Sample app plays a bundled `.json` Lottie file using `RLottieDrawable`. Expect ~5 files to copy plus 1 native binary per ABI.

**Phase 3 — PhotoViewer.** Copy `PhotoViewer` + `VideoPlayer` + `ImageReceiver` + `ImageLoader` + their transitive UI dependencies. Sample app picks a local image/video and opens PhotoViewer over it. This phase is the largest and may need more stubs (or partial copies) for `MessageObject`, `FileLoader`, and `MessagesController`. The Phase 3 spec will inventory exactly what's needed.
