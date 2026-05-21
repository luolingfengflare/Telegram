# `:sdk-media` Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new `:sdk-media` Android library module and a `:sample` Android application that demonstrates an audio waveform visualizer. The sample records 5-second voice clips, plays them back, and shows a circular bezier-curve waveform animation around the play button — reusing Telegram's `AudioVisualizerDrawable` copied verbatim.

**Architecture:** `:sdk-media` contains 2 verbatim copies (`AudioVisualizerDrawable`, `CircleBezierDrawable`) plus 4 minimal stubs (`AndroidUtilities`, `LiteMode`, `SharedConfig`, `Theme`) that preserve the original `org.telegram.*` package names so the copied drawables compile without dragging in the rest of TMessagesProj. `:sample` is a Kotlin Material3 app that uses Android-native `MediaRecorder` + `MediaPlayer` and feeds amplitude samples to the drawable through its public API. `:TMessagesProj` is read-only — never modified.

**Tech Stack:** AGP 8.6.1, Kotlin 1.9.20, Gradle 8.7, Java 11. `:sdk-media` is `com.android.library` (minSdk 21). `:sample` is `com.android.application` (minSdk 24, Kotlin, Material 1.12.0).

---

## File Structure

**New files in `:sdk-media`:**
- `sdk-media/build.gradle.kts`
- `sdk-media/.gitignore`
- `sdk-media/consumer-rules.pro` (empty)
- `sdk-media/proguard-rules.pro` (empty)
- `sdk-media/src/main/AndroidManifest.xml`
- `sdk-media/src/main/java/org/telegram/messenger/AndroidUtilities.java` (stub)
- `sdk-media/src/main/java/org/telegram/messenger/LiteMode.java` (stub)
- `sdk-media/src/main/java/org/telegram/messenger/SharedConfig.java` (stub)
- `sdk-media/src/main/java/org/telegram/ui/ActionBar/Theme.java` (stub)
- `sdk-media/src/main/java/org/telegram/ui/Components/AudioVisualizerDrawable.java` (verbatim copy)
- `sdk-media/src/main/java/org/telegram/ui/Components/CircleBezierDrawable.java` (verbatim copy)

**New files in `:sample`:**
- `sample/build.gradle.kts`
- `sample/.gitignore`
- `sample/proguard-rules.pro` (empty)
- `sample/src/main/AndroidManifest.xml`
- `sample/src/main/java/com/lingyun/app/sample/MainActivity.kt`
- `sample/src/main/java/com/lingyun/app/sample/audio/AudioWaveformActivity.kt`
- `sample/src/main/java/com/lingyun/app/sample/audio/AudioRecorderHelper.kt`
- `sample/src/main/java/com/lingyun/app/sample/audio/WaveformBuffer.kt` (pure-Kotlin amplitude ring buffer, unit-tested)
- `sample/src/main/java/com/lingyun/app/sample/audio/WaveformView.kt`
- `sample/src/test/java/com/lingyun/app/sample/audio/WaveformBufferTest.kt` (JUnit 4)
- `sample/src/main/res/layout/activity_main.xml`
- `sample/src/main/res/layout/activity_audio_waveform.xml`
- `sample/src/main/res/values/strings.xml`
- `sample/src/main/res/values/colors.xml`
- `sample/src/main/res/values/themes.xml`

**Modified files:**
- `settings.gradle` — add 2 lines (`include ':sdk-media'`, `include ':sample'`)

**Never modified:** anything under `TMessagesProj/`.

---

## Pre-flight check

Run these once before starting and confirm each:

- [ ] `git status` shows a clean working tree on `master`, with HEAD at the upstream baseline:
  ```
  git log --oneline -1
  ```
  Expected: a commit on `master` matching `origin/master` (a `update to 12.X.Y` style message). If you see unrelated WIP, stash or commit before proceeding.

- [ ] TMessagesProj baseline builds:
  ```
  ./gradlew :TMessagesProj:assembleDebug
  ```
  Expected: BUILD SUCCESSFUL. This is the regression baseline — at the end of the plan, this same command must still succeed and produce a byte-identical AAR (or near-identical; CI timestamps differ).

- [ ] Source files we'll copy verbatim are at the expected paths:
  ```
  test -f TMessagesProj/src/main/java/org/telegram/ui/Components/AudioVisualizerDrawable.java && echo ok
  test -f TMessagesProj/src/main/java/org/telegram/ui/Components/CircleBezierDrawable.java && echo ok
  ```
  Expected: prints `ok` twice.

---

### Task 1: Create `:sdk-media` Gradle module skeleton

**Files:**
- Create: `sdk-media/.gitignore`
- Create: `sdk-media/consumer-rules.pro`
- Create: `sdk-media/proguard-rules.pro`
- Create: `sdk-media/src/main/AndroidManifest.xml`
- Create: `sdk-media/build.gradle.kts`
- Modify: `settings.gradle` (append one line)

- [ ] **Step 1: Create `sdk-media/.gitignore`**

```
/build
```

- [ ] **Step 2: Create `sdk-media/consumer-rules.pro`**

Empty file (0 bytes).

- [ ] **Step 3: Create `sdk-media/proguard-rules.pro`**

Empty file (0 bytes).

- [ ] **Step 4: Create `sdk-media/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
</manifest>
```

- [ ] **Step 5: Create `sdk-media/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.library")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.lingyun.app.media"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 6: Append `:sdk-media` to `settings.gradle`**

Open `settings.gradle`. After the last existing `include` line (currently `include ':TMessagesProj_AppTests'`), append:

```groovy
include ':sdk-media'
```

- [ ] **Step 7: Verify Gradle recognizes the module**

Run:
```
./gradlew :sdk-media:tasks --no-daemon
```
Expected: prints the standard Android library task list (`assembleDebug`, `testDebugUnitTest`, etc.). No errors about an unknown project.

- [ ] **Step 8: Commit**

```bash
git add sdk-media/ settings.gradle
git commit -m "feat(sdk-media): create empty Android library module"
```

---

### Task 2: Add the four stubs (AndroidUtilities, LiteMode, SharedConfig, Theme)

These are deliberately minimal — they only need to satisfy `AudioVisualizerDrawable`'s and `CircleBezierDrawable`'s imports + the specific static members those classes reference.

**Files:**
- Create: `sdk-media/src/main/java/org/telegram/messenger/AndroidUtilities.java`
- Create: `sdk-media/src/main/java/org/telegram/messenger/LiteMode.java`
- Create: `sdk-media/src/main/java/org/telegram/messenger/SharedConfig.java`
- Create: `sdk-media/src/main/java/org/telegram/ui/ActionBar/Theme.java`

- [ ] **Step 1: Create `AndroidUtilities.java`**

Path: `sdk-media/src/main/java/org/telegram/messenger/AndroidUtilities.java`

```java
package org.telegram.messenger;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

public final class AndroidUtilities {
    private AndroidUtilities() {}

    /** Display density. Host updates via {@link #setDensity(float)} when display metrics change. */
    public static float density = 1f;

    /** Display refresh rate in Hz. Host updates as needed. */
    public static float screenRefreshRate = 60f;

    /** Shared scratch rect — matches Telegram's AndroidUtilities.rectTmp. */
    public static final RectF rectTmp = new RectF();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void setDensity(float value) {
        density = value;
    }

    public static int dp(float value) {
        if (value == 0) return 0;
        return (int) Math.ceil(density * value);
    }

    public static void runOnUIThread(Runnable r) {
        MAIN.post(r);
    }

    public static void runOnUIThread(Runnable r, long delayMs) {
        MAIN.postDelayed(r, delayMs);
    }

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

- [ ] **Step 2: Create `LiteMode.java`**

Path: `sdk-media/src/main/java/org/telegram/messenger/LiteMode.java`

```java
package org.telegram.messenger;

public final class LiteMode {
    private LiteMode() {}

    /** Flag constant used by AudioVisualizerDrawable. Numeric value irrelevant for the stub. */
    public static final int FLAG_CHAT_BACKGROUND = 1;

    /** Default: animation enabled. The sample app can shadow this if it wants opt-out. */
    public static boolean isEnabled(int flag) {
        return true;
    }
}
```

- [ ] **Step 3: Create `SharedConfig.java`**

Path: `sdk-media/src/main/java/org/telegram/messenger/SharedConfig.java`

```java
package org.telegram.messenger;

/**
 * Empty stub. AudioVisualizerDrawable imports this class but does not reference
 * any of its fields. Kept as an empty file so the import resolves.
 */
public final class SharedConfig {
    private SharedConfig() {}
}
```

- [ ] **Step 4: Create `Theme.java`**

Path: `sdk-media/src/main/java/org/telegram/ui/ActionBar/Theme.java`

```java
package org.telegram.ui.ActionBar;

public final class Theme {
    private Theme() {}

    /** Color key for outgoing-message loader spinner. */
    public static final int key_chat_outLoader = 1;

    /** Color key for incoming-message loader spinner. */
    public static final int key_chat_inLoader = 2;

    /**
     * Resource provider passed by callers that want to override default colors.
     * AudioVisualizerDrawable consults this if non-null, otherwise falls back to
     * {@link Theme#getColor(int)}.
     */
    public interface ResourcesProvider {
        int getColor(int key);
    }

    /** Default palette returns Material Blue tones. */
    public static int getColor(int key) {
        switch (key) {
            case key_chat_outLoader: return 0xFF2196F3; // Material Blue 500
            case key_chat_inLoader:  return 0xFF1976D2; // Material Blue 700
            default:                 return 0xFF888888; // medium gray
        }
    }

    /** Provider-aware accessor. Falls through to {@link #getColor(int)} if provider is null. */
    public static int getColor(int key, ResourcesProvider provider) {
        if (provider != null) return provider.getColor(key);
        return getColor(key);
    }
}
```

- [ ] **Step 5: Compile**

Run:
```
./gradlew :sdk-media:compileDebugJavaWithJavac
```
Expected: BUILD SUCCESSFUL. The 4 stubs compile cleanly on their own.

- [ ] **Step 6: Commit**

```bash
git add sdk-media/src/main/java/org/telegram/messenger/ \
        sdk-media/src/main/java/org/telegram/ui/ActionBar/
git commit -m "feat(sdk-media): add minimal stubs for AndroidUtilities/LiteMode/SharedConfig/Theme"
```

---

### Task 3: Verbatim copy of `AudioVisualizerDrawable` and `CircleBezierDrawable`

**Files:**
- Create: `sdk-media/src/main/java/org/telegram/ui/Components/AudioVisualizerDrawable.java` (by copying from TMessagesProj)
- Create: `sdk-media/src/main/java/org/telegram/ui/Components/CircleBezierDrawable.java` (by copying)

- [ ] **Step 1: Ensure destination directory exists**

```bash
mkdir -p sdk-media/src/main/java/org/telegram/ui/Components
```

- [ ] **Step 2: Copy AudioVisualizerDrawable verbatim**

```bash
cp TMessagesProj/src/main/java/org/telegram/ui/Components/AudioVisualizerDrawable.java \
   sdk-media/src/main/java/org/telegram/ui/Components/AudioVisualizerDrawable.java
```

Verify the copy is byte-identical:
```bash
diff TMessagesProj/src/main/java/org/telegram/ui/Components/AudioVisualizerDrawable.java \
     sdk-media/src/main/java/org/telegram/ui/Components/AudioVisualizerDrawable.java
```
Expected: no output (files identical).

- [ ] **Step 3: Copy CircleBezierDrawable verbatim**

```bash
cp TMessagesProj/src/main/java/org/telegram/ui/Components/CircleBezierDrawable.java \
   sdk-media/src/main/java/org/telegram/ui/Components/CircleBezierDrawable.java
```

Verify:
```bash
diff TMessagesProj/src/main/java/org/telegram/ui/Components/CircleBezierDrawable.java \
     sdk-media/src/main/java/org/telegram/ui/Components/CircleBezierDrawable.java
```
Expected: no output.

- [ ] **Step 4: Compile `:sdk-media`**

Run:
```
./gradlew :sdk-media:assembleDebug
```
Expected: BUILD SUCCESSFUL. The verbatim copies' imports (`org.telegram.messenger.AndroidUtilities`, `org.telegram.messenger.LiteMode`, `org.telegram.messenger.SharedConfig`, `org.telegram.ui.ActionBar.Theme`) resolve to the stubs from Task 2.

If compilation fails with "cannot find symbol", that means the drawable references a stub member we didn't include. Read the error, add the missing member to the relevant stub, recompile.

- [ ] **Step 5: Boundary check**

Run:
```bash
grep -rE "TMessagesProj" sdk-media/ 2>/dev/null || echo "  clean"
```
Expected: prints `  clean`.

- [ ] **Step 6: Commit**

```bash
git add sdk-media/src/main/java/org/telegram/ui/Components/
git commit -m "feat(sdk-media): verbatim copy of AudioVisualizerDrawable + CircleBezierDrawable"
```

---

### Task 4: Create `:sample` Gradle module skeleton

**Files:**
- Create: `sample/.gitignore`
- Create: `sample/proguard-rules.pro`
- Create: `sample/build.gradle.kts`
- Create: `sample/src/main/AndroidManifest.xml`
- Create: `sample/src/main/res/values/strings.xml`
- Create: `sample/src/main/res/values/colors.xml`
- Create: `sample/src/main/res/values/themes.xml`
- Modify: `settings.gradle`

- [ ] **Step 1: Create `sample/.gitignore`**

```
/build
```

- [ ] **Step 2: Create `sample/proguard-rules.pro`**

Empty file (0 bytes).

- [ ] **Step 3: Create `sample/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.lingyun.app.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lingyun.app.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":sdk-media"))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 4: Create `sample/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:label="@string/app_name"
        android:theme="@style/Theme.LingyunSample"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".audio.AudioWaveformActivity"
            android:exported="false" />

    </application>
</manifest>
```

- [ ] **Step 5: Create `sample/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Lingyun SDK Demo</string>
    <string name="btn_audio_demo">Audio waveform demo</string>
    <string name="btn_lottie_demo">Lottie demo (Phase 2)</string>
    <string name="btn_photo_demo">Photo viewer demo (Phase 3)</string>
    <string name="coming_soon">Coming in a future phase</string>
    <string name="audio_record_hold">Hold to record</string>
    <string name="audio_play">Play</string>
    <string name="audio_recording">Recording…</string>
    <string name="audio_playing">Playing…</string>
    <string name="audio_permission_needed">Microphone permission needed to record</string>
</resources>
```

- [ ] **Step 6: Create `sample/src/main/res/values/colors.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="waveform_blue">#FF2196F3</color>
    <color name="surface">#FFFFFFFF</color>
    <color name="on_surface">#FF000000</color>
</resources>
```

- [ ] **Step 7: Create `sample/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.LingyunSample" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/waveform_blue</item>
    </style>
</resources>
```

- [ ] **Step 8: Append `:sample` to `settings.gradle`**

After the `include ':sdk-media'` line added in Task 1, append:

```groovy
include ':sample'
```

- [ ] **Step 9: Verify Gradle recognizes the module**

Run:
```
./gradlew :sample:tasks --no-daemon
```
Expected: prints standard Android application task list, no errors.

- [ ] **Step 10: Commit**

```bash
git add sample/ settings.gradle
git commit -m "feat(sample): create empty sample app module with Material theme"
```

---

### Task 5: `MainActivity` with three buttons

A landing screen with three buttons. Phase 1 only wires the first (audio). The other two show a "coming soon" Toast.

**Files:**
- Create: `sample/src/main/res/layout/activity_main.xml`
- Create: `sample/src/main/java/com/lingyun/app/sample/MainActivity.kt`

- [ ] **Step 1: Create `activity_main.xml`**

Path: `sample/src/main/res/layout/activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="32dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_audio"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginVertical="8dp"
        android:text="@string/btn_audio_demo" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_lottie"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginVertical="8dp"
        android:text="@string/btn_lottie_demo" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_photo"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginVertical="8dp"
        android:text="@string/btn_photo_demo" />

</LinearLayout>
```

- [ ] **Step 2: Create `MainActivity.kt`**

Path: `sample/src/main/java/com/lingyun/app/sample/MainActivity.kt`

```kotlin
package com.lingyun.app.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.lingyun.app.sample.audio.AudioWaveformActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.btn_audio).setOnClickListener {
            startActivity(Intent(this, AudioWaveformActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btn_lottie).setOnClickListener {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btn_photo).setOnClickListener {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
    }
}
```

Note: `AudioWaveformActivity` is referenced but doesn't exist yet — the app won't compile until Task 8. That's expected.

- [ ] **Step 3: No compile yet (intentional)**

This task creates only the layout + Activity that references a not-yet-existing class. The next tasks fill in `AudioWaveformActivity` and its helpers. We'll verify compilation after Task 8.

- [ ] **Step 4: Commit**

```bash
git add sample/src/main/res/layout/activity_main.xml \
        sample/src/main/java/com/lingyun/app/sample/MainActivity.kt
git commit -m "feat(sample): add MainActivity with three demo buttons"
```

---

### Task 6: `WaveformBuffer` with TDD unit test

`WaveformBuffer` is a pure-Kotlin ring buffer that holds the last N amplitudes and produces a `FloatArray(8)` in the shape `AudioVisualizerDrawable.setWaveform` expects. Extracting it as a separate class lets us test the math without an Android device.

**Files:**
- Create: `sample/src/test/java/com/lingyun/app/sample/audio/WaveformBufferTest.kt`
- Create: `sample/src/main/java/com/lingyun/app/sample/audio/WaveformBuffer.kt`

- [ ] **Step 1: Write the failing test**

Path: `sample/src/test/java/com/lingyun/app/sample/audio/WaveformBufferTest.kt`

```kotlin
package com.lingyun.app.sample.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WaveformBufferTest {

    @Test
    fun newBuffer_isAllZeros() {
        val buf = WaveformBuffer()
        assertArrayEquals(FloatArray(8) { 0f }, buf.snapshot(playing = false), 0.0001f)
    }

    @Test
    fun push_advancesRingByOne() {
        val buf = WaveformBuffer()
        buf.push(0.5f)
        val snap = buf.snapshot(playing = true)
        // The newest sample lands at index 6 (the "current amplitude" slot)
        assertEquals(0.5f, snap[6], 0.0001f)
        // The "playing" flag lands at index 7
        assertEquals(1f, snap[7], 0.0001f)
    }

    @Test
    fun push_fillsSlots0to5_withRecentHistory_inOrder() {
        val buf = WaveformBuffer()
        // Push 7 samples; the buffer should retain the last 6 in slots 0..5
        listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f).forEach { buf.push(it) }
        val snap = buf.snapshot(playing = true)
        // Slots 0..5 hold the 6 most recent BEFORE the current sample (0.2..0.7
        // since 0.1 has been pushed out), index 0 = oldest, index 5 = newest
        assertEquals(0.2f, snap[0], 0.0001f)
        assertEquals(0.3f, snap[1], 0.0001f)
        assertEquals(0.4f, snap[2], 0.0001f)
        assertEquals(0.5f, snap[3], 0.0001f)
        assertEquals(0.6f, snap[4], 0.0001f)
        assertEquals(0.7f, snap[5], 0.0001f)
        assertEquals(0.7f, snap[6], 0.0001f) // current
        assertEquals(1f, snap[7], 0.0001f)
    }

    @Test
    fun snapshot_clampsInputsTo_0_to_1() {
        val buf = WaveformBuffer()
        buf.push(-0.5f)
        buf.push(1.5f)
        val snap = buf.snapshot(playing = true)
        // Negative clamped to 0, > 1 clamped to 1
        assertEquals(0f, snap[4], 0.0001f) // -0.5 → 0
        assertEquals(1f, snap[5], 0.0001f) // 1.5  → 1
        assertEquals(1f, snap[6], 0.0001f) // current = last pushed (1.5 clamped)
    }

    @Test
    fun playingFlag_isReflectedInSlot7() {
        val buf = WaveformBuffer()
        buf.push(0.3f)
        assertEquals(1f, buf.snapshot(playing = true)[7], 0.0001f)
        assertEquals(0f, buf.snapshot(playing = false)[7], 0.0001f)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:
```
./gradlew :sample:testDebugUnitTest --tests "com.lingyun.app.sample.audio.WaveformBufferTest"
```
Expected: compilation failure (class `WaveformBuffer` doesn't exist).

- [ ] **Step 3: Implement `WaveformBuffer`**

Path: `sample/src/main/java/com/lingyun/app/sample/audio/WaveformBuffer.kt`

```kotlin
package com.lingyun.app.sample.audio

import kotlin.math.max
import kotlin.math.min

/**
 * Pure-Kotlin ring buffer that translates a stream of amplitude samples into
 * the FloatArray(8) shape AudioVisualizerDrawable.setWaveform expects:
 *
 * ```
 * waveform[0..5] = the 6 most-recent amplitudes (index 0 = oldest, 5 = newest)
 * waveform[6]    = the current amplitude (the most recent sample)
 * waveform[7]    = 1f if playing, 0f otherwise
 * ```
 *
 * All amplitudes are clamped to [0, 1].
 *
 * This class is intentionally framework-free (no Android imports) so the
 * audio sampling pipeline can be tested as a pure unit.
 */
class WaveformBuffer {
    private val history = FloatArray(6)
    private var head = 0   // next write position in history[]
    private var current = 0f

    /** Append a new amplitude sample in [0,1]. */
    fun push(amplitude: Float) {
        val clamped = max(0f, min(1f, amplitude))
        history[head] = clamped
        head = (head + 1) % 6
        current = clamped
    }

    /**
     * Build the 8-slot array AudioVisualizerDrawable expects. `playing` controls
     * slot 7 (the "enter progress" gate). The returned array is a fresh copy —
     * the caller may safely hand it to setWaveform without worrying about
     * mutation.
     */
    fun snapshot(playing: Boolean): FloatArray {
        val out = FloatArray(8)
        // Walk history starting at the oldest sample (head, since head is "next write")
        for (i in 0 until 6) {
            out[i] = history[(head + i) % 6]
        }
        out[6] = current
        out[7] = if (playing) 1f else 0f
        return out
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:
```
./gradlew :sample:testDebugUnitTest --tests "com.lingyun.app.sample.audio.WaveformBufferTest"
```
Expected: 5 tests pass, 0 fail.

- [ ] **Step 5: Commit**

```bash
git add sample/src/main/java/com/lingyun/app/sample/audio/WaveformBuffer.kt \
        sample/src/test/java/com/lingyun/app/sample/audio/WaveformBufferTest.kt
git commit -m "feat(sample): add WaveformBuffer with TDD unit tests"
```

---

### Task 7: `WaveformView` (custom View hosting the drawable)

**Files:**
- Create: `sample/src/main/java/com/lingyun/app/sample/audio/WaveformView.kt`

- [ ] **Step 1: Write `WaveformView`**

Path: `sample/src/main/java/com/lingyun/app/sample/audio/WaveformView.kt`

```kotlin
package com.lingyun.app.sample.audio

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import org.telegram.ui.Components.AudioVisualizerDrawable

/**
 * View that hosts an AudioVisualizerDrawable and draws it centered.
 * The view itself owns the buffer; consumers call [pushAmplitude] / [setPlaying]
 * and the next frame picks it up.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val viz = AudioVisualizerDrawable().also { it.setParentView(this) }
    private val buffer = WaveformBuffer()
    private var playing = false

    /** Material Blue 500 — matches our app theme. */
    var strokeColor: Int = 0xFF2196F3.toInt()

    /** Push one amplitude sample in [0,1]. Call at ~50ms cadence while active. */
    fun pushAmplitude(amplitude: Float) {
        buffer.push(amplitude)
        viz.setWaveform(playing, true, buffer.snapshot(playing))
        invalidate()
    }

    fun setPlaying(value: Boolean) {
        playing = value
        viz.setWaveform(value, true, buffer.snapshot(value))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        viz.draw(canvas, cx, cy, strokeColor, 1f, null)
    }
}
```

- [ ] **Step 2: No standalone compile yet**

`WaveformView` is referenced by `AudioWaveformActivity` (Task 8) and `activity_audio_waveform.xml` (Task 8). We compile in Task 8 after the activity is in place.

- [ ] **Step 3: Commit**

```bash
git add sample/src/main/java/com/lingyun/app/sample/audio/WaveformView.kt
git commit -m "feat(sample): add WaveformView (custom View wrapping AudioVisualizerDrawable)"
```

---

### Task 8: `AudioRecorderHelper` (MediaRecorder + MediaPlayer wrapper)

**Files:**
- Create: `sample/src/main/java/com/lingyun/app/sample/audio/AudioRecorderHelper.kt`

- [ ] **Step 1: Write `AudioRecorderHelper`**

Path: `sample/src/main/java/com/lingyun/app/sample/audio/AudioRecorderHelper.kt`

```kotlin
package com.lingyun.app.sample.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * Records to {@code cacheDir/audio-waveform-sample.m4a} and plays it back.
 * Emits amplitude samples (0..1) at a 50ms cadence to [onAmplitude] for as long
 * as a recording or playback is active.
 *
 * State machine:
 *   IDLE → startRecording() → RECORDING → stopRecording() → IDLE
 *   IDLE → startPlayback()  → PLAYING   → stopPlayback()  → IDLE
 *
 * The same [onAmplitude] callback fires during both states. The caller is
 * expected to toggle a "playing" boolean on its WaveformView in response to
 * onStateChange events.
 */
class AudioRecorderHelper(
    private val context: Context,
    private val onAmplitude: (amplitude: Float) -> Unit,
    private val onStateChange: (state: State) -> Unit,
) {

    enum class State { IDLE, RECORDING, PLAYING }

    private val handler = Handler(Looper.getMainLooper())
    private var state: State = State.IDLE
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    /** Read-only access to the current state, for activities that want to drive UI from it. */
    val currentState: State get() = state

    private val outputFile: File
        get() = File(context.cacheDir, "audio-waveform-sample.m4a")

    private val pollLoop = object : Runnable {
        override fun run() {
            val amp = currentAmplitude()
            onAmplitude(amp)
            if (state != State.IDLE) {
                handler.postDelayed(this, 50)
            }
        }
    }

    fun hasRecording(): Boolean = outputFile.exists() && outputFile.length() > 0L

    fun startRecording() {
        if (state != State.IDLE) return
        outputFile.parentFile?.mkdirs()

        @Suppress("DEPRECATION")
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        transition(State.RECORDING)
    }

    fun stopRecording() {
        if (state != State.RECORDING) return
        try {
            recorder?.apply { stop(); release() }
        } catch (ignored: Exception) {
            // MediaRecorder.stop() throws if called too soon after start(); ignore
        }
        recorder = null
        transition(State.IDLE)
    }

    fun startPlayback() {
        if (state != State.IDLE || !hasRecording()) return
        player = MediaPlayer().apply {
            setDataSource(outputFile.absolutePath)
            prepare()
            setOnCompletionListener { stopPlayback() }
            start()
        }
        transition(State.PLAYING)
    }

    fun stopPlayback() {
        if (state != State.PLAYING) return
        try {
            player?.apply { stop(); release() }
        } catch (ignored: Exception) {}
        player = null
        transition(State.IDLE)
    }

    fun release() {
        stopRecording()
        stopPlayback()
        handler.removeCallbacks(pollLoop)
    }

    private fun transition(next: State) {
        state = next
        onStateChange(next)
        handler.removeCallbacks(pollLoop)
        if (next != State.IDLE) {
            handler.post(pollLoop)
        } else {
            // Final amplitude tick = 0 to reset the visualizer
            onAmplitude(0f)
        }
    }

    /** Read the current amplitude (0..1) from whichever source is active. */
    private fun currentAmplitude(): Float = when (state) {
        State.RECORDING -> {
            val raw = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
            // MediaRecorder.getMaxAmplitude returns 0..32767; normalize to 0..1
            (raw / 32767f).coerceIn(0f, 1f)
        }
        State.PLAYING -> {
            // MediaPlayer doesn't expose live amplitude; synthesize a gentle pulse
            // so the waveform still shows life. Random in [0.3, 0.9].
            0.3f + Math.random().toFloat() * 0.6f
        }
        State.IDLE -> 0f
    }
}
```

- [ ] **Step 2: No compile yet (deferred to Task 9)**

The next task creates the Activity that uses both `WaveformView` and `AudioRecorderHelper`. Compile happens then.

- [ ] **Step 3: Commit**

```bash
git add sample/src/main/java/com/lingyun/app/sample/audio/AudioRecorderHelper.kt
git commit -m "feat(sample): add AudioRecorderHelper (MediaRecorder + MediaPlayer wrapper)"
```

---

### Task 9: `AudioWaveformActivity` (tying everything together)

**Files:**
- Create: `sample/src/main/res/layout/activity_audio_waveform.xml`
- Create: `sample/src/main/java/com/lingyun/app/sample/audio/AudioWaveformActivity.kt`

- [ ] **Step 1: Create `activity_audio_waveform.xml`**

Path: `sample/src/main/res/layout/activity_audio_waveform.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/surface">

    <com.lingyun.app.sample.audio.WaveformView
        android:id="@+id/waveform_view"
        android:layout_width="240dp"
        android:layout_height="240dp"
        android:layout_gravity="center" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_record"
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:layout_gravity="center"
        android:text="●"
        android:textSize="32sp"
        app:cornerRadius="36dp"
        xmlns:app="http://schemas.android.com/apk/res-auto" />

    <TextView
        android:id="@+id/lbl_status"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal|bottom"
        android:layout_marginBottom="64dp"
        android:textSize="16sp"
        android:text="@string/audio_record_hold" />

</FrameLayout>
```

- [ ] **Step 2: Create `AudioWaveformActivity.kt`**

Path: `sample/src/main/java/com/lingyun/app/sample/audio/AudioWaveformActivity.kt`

```kotlin
package com.lingyun.app.sample.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.lingyun.app.sample.R

class AudioWaveformActivity : AppCompatActivity() {

    private lateinit var waveform: WaveformView
    private lateinit var button: MaterialButton
    private lateinit var status: TextView
    private lateinit var helper: AudioRecorderHelper

    private var hasPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_waveform)

        waveform = findViewById(R.id.waveform_view)
        button = findViewById(R.id.btn_record)
        status = findViewById(R.id.lbl_status)

        helper = AudioRecorderHelper(
            context = this,
            onAmplitude = { amp -> waveform.pushAmplitude(amp) },
            onStateChange = { newState ->
                when (newState) {
                    AudioRecorderHelper.State.IDLE -> {
                        waveform.setPlaying(false)
                        status.text = if (helper.hasRecording())
                            getString(R.string.audio_play) else getString(R.string.audio_record_hold)
                        button.text = if (helper.hasRecording()) "▶" else "●"
                    }
                    AudioRecorderHelper.State.RECORDING -> {
                        waveform.setPlaying(true)
                        status.setText(R.string.audio_recording)
                        button.text = "●"
                    }
                    AudioRecorderHelper.State.PLAYING -> {
                        waveform.setPlaying(true)
                        status.setText(R.string.audio_playing)
                        button.text = "■"
                    }
                }
            }
        )

        checkAndRequestPermission()
        wireButton()
    }

    private fun checkAndRequestPermission() {
        hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            hasPermission = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Snackbar.make(waveform, R.string.audio_permission_needed, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { checkAndRequestPermission() }
                    .show()
            }
        }
    }

    private fun wireButton() {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    when (helper.currentState) {
                        AudioRecorderHelper.State.PLAYING -> {
                            // Tap during playback toggles playback off
                            helper.stopPlayback()
                        }
                        AudioRecorderHelper.State.IDLE -> {
                            if (helper.hasRecording()) {
                                // Existing recording: tap to play
                                helper.startPlayback()
                            } else if (hasPermission) {
                                // No recording yet: press-and-hold to record
                                helper.startRecording()
                            }
                        }
                        AudioRecorderHelper.State.RECORDING -> {
                            // Already recording — DOWN is ignored, UP will stop it
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Stop recording on release. No-op if not currently recording.
                    helper.stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    override fun onStop() {
        super.onStop()
        helper.release()
    }
}
```

- [ ] **Step 3: Compile the whole sample app**

Run:
```
./gradlew :sample:assembleDebug
```
Expected: BUILD SUCCESSFUL. An APK at `sample/build/outputs/apk/debug/sample-debug.apk`.

If you see errors about missing `R` references (`R.id.waveform_view` etc.), confirm all resource files from previous tasks were created with the names listed.

- [ ] **Step 4: Run unit tests one final time (regression)**

```
./gradlew :sample:testDebugUnitTest
```
Expected: 5 WaveformBuffer tests pass.

- [ ] **Step 5: Commit**

```bash
git add sample/src/main/res/layout/activity_audio_waveform.xml \
        sample/src/main/java/com/lingyun/app/sample/audio/AudioWaveformActivity.kt
git commit -m "feat(sample): add AudioWaveformActivity wiring view + recorder helper"
```

---

### Task 10: Boundary check + TMessagesProj regression check + smoke test

This is the verification gate that the spec called out under "Definition of done".

- [ ] **Step 1: Architecture / boundary check**

```bash
echo "=== TMessagesProj refs in sdk-media or sample (should be empty) ==="
grep -rE "TMessagesProj" sdk-media/ sample/ 2>/dev/null || echo "  clean"

echo "=== sdk-media or sample refs in TMessagesProj (should be empty) ==="
grep -rE "(sdk-media|com\.lingyun\.app)" TMessagesProj/ 2>/dev/null || echo "  clean"
```
Expected: both print `  clean`.

- [ ] **Step 2: TMessagesProj regression check**

```
./gradlew :TMessagesProj:assembleDebug
```
Expected: BUILD SUCCESSFUL — same as the pre-flight baseline. The TMessagesProj AAR is unaffected by the new modules.

- [ ] **Step 3: All-module clean build**

```
./gradlew :sdk-media:assembleDebug :sample:assembleDebug :TMessagesProj:assembleDebug --rerun-tasks
```
Expected: BUILD SUCCESSFUL. The three modules build independently and don't interfere.

- [ ] **Step 4: Install on device or emulator**

```
./gradlew :sample:installDebug
```
Or with `adb install` against the APK path printed in Task 9. Open the "Lingyun SDK Demo" app from the launcher.

- [ ] **Step 5: Manual smoke test**

Walk through these steps; each must succeed:

1. App launches → MainActivity visible with three buttons labelled "Audio waveform demo", "Lottie demo (Phase 2)", "Photo viewer demo (Phase 3)".
2. Tap the Lottie or Photo button → Toast appears: "Coming in a future phase".
3. Tap "Audio waveform demo" → AudioWaveformActivity opens.
4. System permission dialog appears asking for microphone access → grant it.
5. The screen shows a circular record button overlaid on a 240dp waveform area, with the status text "Hold to record".
6. Press and hold the record button → status changes to "Recording…", the waveform animates with blue concentric bezier rings around the button.
7. Release the button → status changes to "Play", button glyph becomes ▶, waveform stops animating.
8. Tap the play button → status changes to "Playing…", glyph becomes ■, waveform animates again (synthesized pulse pattern), audio plays through the device speakers.
9. Playback completes → state returns to "Play"; tap again to replay.
10. Back-press → returns to MainActivity. Re-open → previous recording is still cached (button starts in "Play" mode).

If any step fails, do not mark this PR done; diagnose and fix before continuing.

- [ ] **Step 6: No commit** — verification only.

---

### Task 11: Document module + final commit

**Files:**
- Create: `sdk-media/README.md`
- Create: `sample/README.md`

- [ ] **Step 1: Create `sdk-media/README.md`**

```markdown
# :sdk-media

Media UI library for the Lingyun fork of Telegram for Android. Code is
copied from `:TMessagesProj` into this module, preserving original
`org.telegram.*` package names. The four files under
`org/telegram/messenger/` and `org/telegram/ui/ActionBar/Theme.java` are
minimal stubs we wrote to satisfy the copied drawables' imports without
dragging in the rest of the Telegram client.

## Phase 1 contents

| File | Origin |
|---|---|
| `org/telegram/ui/Components/AudioVisualizerDrawable.java` | verbatim copy from TMessagesProj |
| `org/telegram/ui/Components/CircleBezierDrawable.java` | verbatim copy from TMessagesProj |
| `org/telegram/messenger/AndroidUtilities.java` | stub (dp, runOnUIThread, recycleBitmaps) |
| `org/telegram/messenger/LiteMode.java` | stub (FLAG_CHAT_BACKGROUND, isEnabled always true) |
| `org/telegram/messenger/SharedConfig.java` | stub (empty) |
| `org/telegram/ui/ActionBar/Theme.java` | stub (ResourcesProvider, getColor) |

## Rules

- This module MUST NOT depend on `:TMessagesProj` or anything inside it.
- When adding new Telegram-side code, prefer verbatim copy; stubs are for
  classes that pull in massive dependency trees (controllers, TLRPC, etc.).
- Public API surface is whatever the copied/stubbed classes expose. There
  is no separate SDK-facing API in Phase 1.

## Build

```
./gradlew :sdk-media:assembleDebug
./gradlew :sdk-media:testDebugUnitTest
```
```

- [ ] **Step 2: Create `sample/README.md`**

```markdown
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
```

- [ ] **Step 3: Commit**

```bash
git add sdk-media/README.md sample/README.md
git commit -m "docs: add README files for :sdk-media and :sample"
```

- [ ] **Step 4: Final inventory**

```bash
echo "=== files added/modified across this PR ==="
git log --name-only --pretty=format: master..HEAD 2>/dev/null | sort -u | head -40
echo ""
echo "=== commit messages ==="
git log --oneline master..HEAD
```
Expected: 11 commits matching the messages from Tasks 1, 2, 3, 4, 5, 6, 7, 8, 9, 11 (Task 10 is verification-only with no commit), plus this Task 11 docs commit.

---

## Out of scope (verified absent)

- Native libraries (`librtmessages.so`, etc.) — none in Phase 1.
- Lottie / `RLottieDrawable` — Phase 2.
- PhotoViewer / `VideoPlayer` — Phase 3.
- Maven/AAR publishing — `:sdk-media` is a Gradle project dependency only.
- `:sample` instrumentation tests — manual smoke test (Task 10 Step 5) is the gate.
- Modifications to `TMessagesProj/` of any kind.

## Effort estimate

For a developer familiar with Android Gradle + Kotlin: **~3-4 hours** end-to-end. The TDD task (Task 6) is the only one that involves non-trivial logic; everything else is configuration and connecting prebuilt parts.
