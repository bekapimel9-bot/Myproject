# GameLingo Offline - Android Game Screen Translator

GameLingo Offline is a native Android gaming utility engineered to translate in-game dialogue and foreign text in real time without forcing players to switch away from active gameplay. Built with Jetpack Compose, Material 3, and a high-performance offline pipeline, it combines on-device OCR and machine translation with a low-latency floating overlay and direct in-place subtitle replacement.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during the planning phase:
> - **Engine Pipeline**: Hybrid on-device Google ML Kit Text Recognition and ML Kit On-Device Translation, with an optional Gemini cloud fallback for advanced nuance when connected.
> - **Default Overlay Mode**: Direct in-place text replacement overlay rendered directly above recognized game dialogue using OCR bounding boxes, with toggle to a floating translation card.
> - **Primary Language Pair**: English to Indonesian by default, with pre-configured models and downloadable support for Japanese, Chinese, and Korean.
> - **Monetization & Access**: 100% unlocked native application without subscriptions, paywalls, ads, or account requirements.

---

## 1. Overview & Core Concept

- **What It Does**: Overlays a draggable floating bubble above any running game. When triggered (manually or via automated periodic capture), it takes a screen snapshot via Android's `MediaProjection` API, extracts text lines and bounding boxes using Google ML Kit OCR, translates text offline into Indonesian using ML Kit's local neural models, and projects translated subtitles right over the original dialogue box.
- **Target Audience**: Gamers playing untranslated or localized RPGs, visual novels, strategy games, and foreign titles on Android phones and tablets.
- **Key Value**: Frictionless, zero-switch comprehension with genuine offline capability, customizable screen region selection, and zero performance drag on active games.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard & Permission Onboarding**:
   - The user opens GameLingo Offline and is greeted by a sleek gaming command center in Indonesian.
   - Status chips display the state of the Overlay (`SYSTEM_ALERT_WINDOW`) and Screen Capture (`MediaProjection`) permissions.
   - Tapping "Mulai Layanan" prompts the necessary permissions.
2. **In-Game Floating Control & Region Selection**:
   - The user launches a game. A compact, translucent floating orb sits at the screen edge without obscuring HUD controls.
   - Tapping the orb expands a quick-action radial palette:
     - 🎯 **Pilih Area Layar** (Region Selector): Dimmed overlay where the user drags a selection box over the game's dialogue box.
     - ⚡ **Terjemahkan Sekarang** (Manual Capture & Translate).
     - 🔄 **Terjemahan Otomatis** (Periodic background scanning).
     - 👁️ **Mode Tampilan** (In-place subtitle vs. draggable floating card).
     - ❌ **Hentikan** (Dismiss overlay).
3. **In-Place Dialogue Overlay**:
   - Translated text is rendered directly over the dialogue area with customizable high-contrast dark backing, crisp typography, and instant copy/readout actions.
4. **History & Model Management**:
   - Full search and audit log of translated dialogue saved in local Room database.
   - Offline Language Model Manager to view, download, and delete local ML Kit translation packages.

### Visual Identity & Theme

- **Aesthetic Direction**: High-tech gaming HUD / cyber-utility with dark theme defaults.
- **Color Palette**:
  - `Background`: Deep Charcoal / Obsidian (`#0F1016`)
  - `Surface`: Elevated Dark Violet-Black (`#181924`)
  - `Primary / Accent`: Neon Cyan (`#00E5FF`) and Electric Violet (`#7C4DFF`)
  - `Text`: High-contrast stark white (`#F0F2F8`) and muted silver (`#9EA3B0`)
  - `Success / Offline Ready`: Bright Emerald (`#00E676`)
- **Typography & Hierarchy**:
  - Bold, clean sans-serif typography with tight, legible letter spacing for subtitles and high glanceability during fast-paced games.
- **Interactive Feedback**:
  - Haptic feedback on capture initiation, subtle glowing outline on the active region box, and spring animations on floating bubble expansion.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Google ML Kit On-Device OCR & Translation**
  - *Chosen Approach*: Official `com.google.mlkit:text-recognition` and `com.google.mlkit:translate` on-device models.
  - *Why*: True offline inference once the model is downloaded (~30MB per language), zero network latency, robust bounding box detection, and official Android lifecycle support.
  - *Alternatives Considered*: Tesseract (heavy NDK binaries, higher latency) and cloud-only APIs (incompatible with offline requirement).
- **Decision 2: Foreground Service with MediaProjection & VirtualDisplay**
  - *Chosen Approach*: A dedicated Android Foreground Service (`ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`) managing `MediaProjection`, `VirtualDisplay`, and `ImageReader`.
  - *Why*: Conforms strictly to Android 14+ background execution policies, captures frames on demand without continuous video encoding overhead, and frees memory immediately after OCR.
- **Decision 3: Room Database for Local Translation History**
  - *Chosen Approach*: Device-local SQLite via Room for logging timestamps, source text, translated text, and language pairs.
  - *Why*: Private, fast, searchable, and works 100% offline without requiring account setup or internet connection.
- **Decision 4: DataStore Preferences for Fast Runtime Settings**
  - *Chosen Approach*: Jetpack DataStore Preferences for user configs (recognition interval, region coordinates, overlay opacity, font size, default languages).

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    GameLingo Offline App                     │
├──────────────────────────────┬───────────────────────────────┤
│        Main Activity         │     Floating Service HUD      │
│   (Dashboard, Model Mgr,     │  (WindowManager Floating Orb, │
│     History & Settings)      │  In-Place Subtitles & Region) │
└──────────────┬───────────────┴───────────────┬───────────────┘
               │                               │
               ▼                               ▼
┌──────────────────────────────────────────────────────────────┐
│             TranslationCoordinator & CaptureService           │
│  - MediaProjection Frame Capture (ImageReader / Bitmap)       │
│  - Crop to User Selection Region Rect                        │
│  - Diff / Deduplication Engine (Levenshtein & Hash)          │
└──────────────────────────────┬───────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
┌──────────────────────────────┐ ┌─────────────────────────────┐
│      ML Kit OCR Engine       │ │   Offline Translate Engine  │
│ - TextBlock / Line Detection │ │ - ML Kit Translate (Local)  │
│ - BoundingBox Normalization  │ │ - Model Download Manager    │
└──────────────┬───────────────┘ └─────────────┬───────────────┘
               │                               │
               └───────────────┬───────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                      Data & Persistence                      │
│ - Room Database: TranslationHistoryEntity & Dao              │
│ - DataStore: AppPreferencesRepository (Interval, Region, UI) │
└──────────────────────────────────────────────────────────────┘
```

### Data Models & Entities

- **`TranslationHistoryEntity`**:
  - `id: Long` (auto-increment primary key)
  - `originalText: String`
  - `translatedText: String`
  - `sourceLanguage: String`
  - `targetLanguage: String`
  - `timestamp: Long`
  - `engineUsed: String` (e.g., "ML_KIT_OFFLINE", "GEMINI_CLOUD")
- **`ScreenRegion`**:
  - `leftRatio: Float`, `topRatio: Float`, `rightRatio: Float`, `bottomRatio: Float` (normalized 0.0f..1.0f to survive rotation and multi-window resizing).
- **`RecognizedDialogue`**:
  - `text: String`, `translatedText: String`, `boundingBox: Rect`, `confidence: Float`.

### Execution Phases

1. **Phase 1: Project Setup & Dependencies**: Configure `libs.versions.toml` and `app/build.gradle.kts` with ML Kit Text Recognition, ML Kit Translate, DataStore Preferences, and Room.
2. **Phase 2: Core Data & Engine Layers**: Implement `AppDatabase`, `HistoryDao`, `HistoryRepository`, `SettingsRepository`, `OcrEngine`, and `TranslationEngine`.
3. **Phase 3: Screen Capture & MediaProjection**: Build `ScreenCaptureManager` and `FloatingTranslatorService` with official Android Foreground Service declarations.
4. **Phase 4: Floating WindowManager UI**: Implement the draggable floating bubble, the in-place dialogue overlay layer, and the interactive drag-to-select region frame.
5. **Phase 5: Main Dashboard & Model Management**: Complete the Indonesian-first Compose dashboard with real-time status chips, model download progress, history viewer, and settings sheet.
6. **Phase 6: Verification & Polish**: Ensure end-to-end compilation with `compile_applet`, robust unit/Robolectric test cases, and error recovery.
