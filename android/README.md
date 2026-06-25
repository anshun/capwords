# CapWords (Android)

Offline "photograph an object → recognize → translate to English + Chinese →
collect as a sticker" vocabulary app. Rebuilds the flow of `CapWords.mp4` with
original assets and an open-vocabulary on-device recognizer.

## Status
- **Phase 1 (this scaffold):** full UI + flow runs end-to-end. Recognition uses the
  ML Kit on-device labeler (~400 classes) as a placeholder; segmentation uses ML Kit
  Subject Segmentation with a graceful full-frame fallback.
- **Phase 3 (drop-in):** add the MobileCLIP assets (see `app/src/main/assets/README.md`)
  and the app automatically upgrades to ~20k-noun open-vocabulary recognition. No code change.

## Build
Open `android/` in Android Studio (Koala+), let it sync, run on a device with a camera.

CLI (needs a local Gradle 8.9+ if the wrapper jar isn't present):
```bash
cd android
gradle wrapper        # generates gradle/wrapper/gradle-wrapper.jar once
./gradlew assembleDebug
```

> The binary `gradle-wrapper.jar` is intentionally not committed here; Android Studio
> or `gradle wrapper` regenerates it.

## Architecture
- **UI:** Jetpack Compose, single-Activity, Navigation-Compose. Screens: Camera →
  Capture → Recognize → Gallery.
- **Camera:** CameraX (Preview + ImageAnalysis for live labels + ImageCapture).
- **ML:** `ml/Recognizer` (MlKit ↔ MobileCLIP), `ml/Segmenter` (MlKit ↔ U²-Net),
  `ml/Translator` (asset `words.tsv` ↔ seed map).
- **Data:** Room (`words` table) + cut-out PNGs in app-internal storage.
- **TTS:** Android `TextToSpeech`, English + zh-TW/zh-CN.

## How "tens of thousands of objects, offline" works
The image is encoded once by MobileCLIP and matched (cosine similarity) against a
pre-computed table of ~20k English-noun text embeddings bundled in `assets/`.
Breadth is a function of the table size, not the model — ship a bigger table to
support more words. See `tools/` for the pipeline that builds it.
