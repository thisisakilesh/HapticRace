# HapticRace 🏁⚡
### Telemetry-Driven Synchronized Haptic Video Player for Android

**HapticRace** is a native Android application that bridges motorsport video playback with synchronized, high-fidelity tactile feedback. Built with **Kotlin**, **Jetpack Compose**, and **Android Media3 (ExoPlayer)**, it allows users to experience the visceral physical sensation of high-speed racing—from subtle idle engine vibrations and snappy upshifts to aggressive rumble kerbs and high-G impacts—mapped to exact millisecond timestamps on the video timeline.

---

## 🏎️ Core Architectural Overview

```
com.hapticrace.app/
├── haptic/
│   ├── HapticCapabilities.kt   # Hardware vibration detection (vibrator present, amplitude modulation, API level)
│   ├── HapticEventType.kt      # Presets (ENGINE, GEAR_SHIFT, BRAKE, KERB, IMPACT, CUSTOM)
│   ├── HapticIntensity.kt      # Amplitude scaling (LOW 0.5x, MEDIUM 0.8x, HIGH 1.0x)
│   ├── HapticPattern.kt        # Multi-stage VibrationEffect waveforms & fallback patterns
│   └── HapticManager.kt        # Dedicated vibration engine with debouncing, throttle, safety limits
├── race/
│   ├── VideoTimeline.kt        # Reactive event collection and timeline sorting
│   ├── TimelineEvent.kt        # UI mapping, pin styling, and telemetry colors
│   └── PlaybackSyncManager.kt  # High-precision player sync, forward suppression, backward re-arming
├── model/
│   └── HapticVideo.kt          # Video timeline project model with JSON serialization
├── data/
│   └── TimelineRepository.kt   # Local storage JSON persistence with built-in demo track
└── ui/
    ├── theme/                  # Dark motorsport aesthetic (Carbon, Hyper Cyan, Amber, Crimson)
    ├── components/
    │   ├── VideoPlayerView.kt          # Media3 PlayerView Compose wrapper
    │   ├── TimelineScrubber.kt         # Interactive canvas timeline with markers and playhead
    │   ├── HapticTelemetryOverlay.kt   # Live HUD waveform flash during haptic trigger
    │   └── EventBadge.kt               # Styled preset badges
    ├── screens/
    │   ├── HomeScreen.kt               # Screen 1: Status, hardware check, video tracks
    │   ├── VideoImportScreen.kt        # Screen 2: Storage Access Framework picker & metadata
    │   ├── HapticEditorScreen.kt       # Screen 3: Precision timeline editor & fine-tuning
    │   └── PlaybackScreen.kt           # Screen 4: Fullscreen immersive cinema playback
    └── Navigation.kt                   # Jetpack Navigation Compose graph
```

---

## 📱 Screen Breakdown

### Screen 1 — Home
- **Hardware Status Banner**: Real-time detection displaying *"Haptic feedback available"* or *"Haptic feedback unavailable"*, with an interactive hardware sheet indicating linear amplitude control support and Android API level.
- **Master Controls**: Global Haptic switch and global intensity tuning (LOW / MEDIUM / HIGH).
- **Import Video Hero**: One-touch entry into video import.
- **My Haptic Videos**: List of saved video projects with duration, event counts, quick badges, Edit, Play, and Delete options.
- **Pre-loaded Demo**: Comes bundled with *Apex Sprint GP* so the app can be immediately tested out-of-the-box.

### Screen 2 — Video Import
- **Storage Access Framework**: Opens Android's native file picker (`OpenDocument` with `video/*` MIME filter) for safe streaming without redundant disk duplication.
- **Persistable URI Permissions**: Automatically requests and persists URI read permissions across app restarts.
- **Metadata Inspection**: Dynamically extracts video title, duration, and first-frame video thumbnail using `MediaMetadataRetriever`.
- **Instant Project Creation**: Initializes project timeline and launches the Editor.

### Screen 3 — Haptic Editor
- **Video Preview Player**: Embedded Media3 player with frame-accurate timestamp display (`MM:SS.mmm`).
- **Interactive Timeline Scrubber**:
  - Horizontal canvas spanning the duration of the clip.
  - Pin markers color-coded for each haptic preset.
  - Draggable playhead for scrubbing and frame positioning.
- **Preset Buttons**:
  - `[ ENGINE ]`: Low-frequency 3-pulse rumble.
  - `[ GEAR SHIFT ]`: Crisp 45ms snappy punch.
  - `[ BRAKE ]`: Deceleration ramp waveform.
  - `[ KERB ]`: Alternating 8-step rumble-strip chatter.
  - `[ IMPACT ]`: Dual-stage violent collision pulse.
  - `[ CUSTOM ]`: User-configurable duration and amplitude.
- **Event Inspector**:
  - Fine-tune timestamp with millisecond buttons (`-200ms`, `-50ms`, `+50ms`, `+200ms`).
  - Modify intensity and duration.
  - **Live Preview Button** to immediately feel the vibration before saving.
  - Delete event.

### Screen 4 — Immersive Haptic Playback
- **True Fullscreen Experience**: Minimalist cinema mode with auto-hiding HUD controls (fades out after 3.5 seconds).
- **Synchronized Playback**:
  - High-precision 20ms position polling loop against the timeline.
  - Triggers events **exactly once** when their timestamp is reached.
  - **Backward Seek**: Automatically clears triggered state so past events trigger again when reached.
  - **Forward Seek**: Suppresses intermediate skipped events, preventing unwanted burst vibrations.
- **Telemetry HUD**: Real-time animated G-meter / waveform badge flashes on screen whenever a haptic event fires.
- **On-the-Fly Tuning**: Instant toggle between Haptic ON/OFF and intensity adjustment without pausing playback.

---

## 📳 Haptic Hardware & Safety Architecture

1. **Hardware Detection**:
   Queries `Vibrator` / `VibratorManager` on startup. If the device lacks vibration motors (e.g. some tablets or emulators), the app gracefully switches to Visual-Only Mode (`Haptic feedback unavailable`), disabling vibration calls while video playback continues seamlessly.
2. **Linear Amplitude Control**:
   On devices supporting `hasAmplitudeControl()` (API 26+), HapticRace creates scaled waveforms using `VibrationEffect.createWaveform(timings, amplitudes, -1)` and `VibrationEffect.createOneShot(duration, amplitude)`.
3. **Legacy Fallback**:
   On devices without amplitude control, a duty-cycle pulsed timing pattern is automatically used.
4. **Motor Protection & Battery Conservation**:
   All trigger invocations pass through `HapticManager.triggerEvent()` with a hardware debounce threshold (minimum 35ms interval) to avoid stressing the actuator or draining battery.

---

## 🛠️ How to Build and Run the App

### Requirements
- **Android Studio** (Koala / Ladybug / Narwhal or any recent version)
- **Android SDK** API 34 (compileSdk 34, minSdk 26)
- **JDK 17**

### Option 1: Using Android Studio
1. Open Android Studio.
2. Select **File > Open** and choose the `HapticRace` folder (`d:\HapticRace`).
3. Allow Gradle to sync dependencies.
4. Select a connected physical Android device or emulator.
5. Click **Run > Run 'app'** (or press `Shift + F10`).

### Option 2: Using the Command Line
```bash
# Build the Debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install directly on a connected device via ADB
./gradlew installDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🏎️ How to Test on a Physical Android Phone

1. **Enable Developer Options**:
   - Go to **Settings > About phone**.
   - Tap **Build number** 7 times.
   - Go to **Settings > System > Developer options** and enable **USB debugging**.
2. **Connect Phone**:
   - Connect via USB cable. Verify with `adb devices`.
3. **Launch HapticRace**:
   - Check the **Hardware Status Bar** on the Home screen—it will display *"Haptic feedback available"* with a green indicator.
   - Tap the bar to see your phone's vibration capabilities (linear amplitude control, API level).
4. **Test the Demo Race**:
   - Tap **"Apex Sprint GP (Demo Showcase)"** on the Home screen.
   - Tap **"PLAY RACE"**.
   - Feel the synchronized haptics at:
     - `00:04.20`: Engine idle revs
     - `00:07.85`: 2nd gear upshift kick
     - `00:12.40`: Heavy Turn 1 braking
     - `00:14.10`: Apex rumble kerb
     - `00:18.60`: Sidepod impact shock
5. **Test Seeking**:
   - Scrub the slider backward past a marker—observe the event triggers again when reached.
   - Scrub forward past several markers—notice the phone does **not** vibrate violently for skipped markers.
6. **Import Your Own Video**:
   - Tap **"IMPORT LOCAL VIDEO"**.
   - Pick an onboard racing clip or action video from your gallery/storage.
   - Play the video, tap `[ GEAR SHIFT ]` or `[ BRAKE ]` while watching to drop markers in real time.
   - Tap **"RACE (PLAY)"** to enjoy your personalized haptic track!

---

## 🔍 Known Limitations (V1)
- **Manual Timeline Mapping**: In V1, haptic points are created manually by the user or pre-loaded via timeline presets (no AI audio/optical-flow automatic trigger generation in V1).
- **DRM Encrypted Streams**: Standard local and unencrypted formats (MP4, MKV, WebM, 3GP) are supported; widevine-protected DRM streams are not mapped.
- **Physical Motors on Budget Devices**: Basic ERM (Eccentric Rotating Mass) motors found in budget hardware have longer spin-up latency (~30-50ms) compared to modern X-axis LRA (Linear Resonant Actuators) haptic motors.

---

## 🚀 Recommended V2 Roadmap
1. **Audio Transient Analysis**: Auto-detect gear shifts and impacts using on-device FFT transient peak detection from the video's audio stream.
2. **Track Telemetry Export/Import**: Export and import haptic tracks as lightweight `.haptic` JSON bundles to share tracks with friends.
3. **Haptic Waveform Canvas Editor**: Visual curve editor allowing users to draw custom amplitude envelopes.
4. **Bluetooth Gamepad / DualSense Support**: Mirror haptic telemetry to external racing wheels and PlayStation DualSense haptic triggers.
