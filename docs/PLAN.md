# CapWords 離線 Android App — 完整實作計畫

> 目標：複製目錄下 `CapWords.mp4` 的畫面與流程，做出一個**離線**的 Android App。
> 離線模型可即時辨識實物並翻成**中英文**，支援英文字典上**多數具體物體**的辨識（目標約 2 萬個名詞，而非僅數十個）。

---

## 0. 決策紀錄（已與使用者確認）

| 項目 | 決定 |
|---|---|
| 中文版本 | **繁體 + 簡體都收**（詞庫雙份、TTS 可切 zh-TW / zh-CN） |
| 辨識廣度 | **精選 ~2 萬具體名詞**（MobileCLIP 開放詞彙），廣度與準確度最佳平衡 |
| 推進方式 | 先寫完整計畫（本文件），再從 Phase 1 開工 |

---

## 1. 來源 App 流程分析（自 CapWords.mp4 擷取）

CapWords 是「拍實物 → 辨識 → 摳圖成貼紙 → 翻譯收藏」的單字學習 App。影片為直式手機畫面（588×1274，約 26 秒）。共五個畫面：

1. **即時相機**：CameraX 預覽，中央對焦框，提示「請將物體放入框內」（影片有多語：英/日/韓），頂部顯示日期，底部彩虹快門鍵 + 相簿縮圖。
2. **拍攝 / 確認**：凍結畫面，把主體從背景**摳出**（去背），底部工具列為 裁切 / ✓ / ✕。
3. **辨識 + 翻譯**：辨識物體（Donut、Toast、Bread…）並翻成目標語言，帶喇叭發音鍵；下方「結果不如預期？點一下調整」。
4. **單字卡**：去背貼紙 + 英文字 + 母語翻譯 + TTS 發音。
5. **收藏相簿**：依日期分組（如 Feb 23、8 Words），滿版瀑布流展示去背貼紙與單字。

> 註：UI / 互動流程屬通用設計樣式，本專案會以**原創素材**重建，不照搬來源 App 的圖檔或資源。

---

## 2. 技術選型

| 層面 | 選擇 | 理由 |
|---|---|---|
| 語言 / UI | Kotlin + **Jetpack Compose** | 自訂動畫、瀑布流、貼紙卡最適合 Compose |
| 相機 | **CameraX**（Preview + ImageAnalysis） | 即時預覽 + 節流跑辨識 |
| 端上推論 | **LiteRT (TFLite)** + GPU / NNAPI delegate | 手機端即時跑 MobileCLIP 影像編碼器 |
| 資料庫 | **Room** | 收藏單字卡、貼紙、日期分組 |
| 發音 | Android `TextToSpeech`（離線語音包） | 中、英皆可離線 |
| 最低 SDK | minSdk 26 / target 34 | 涵蓋絕大多數裝置、支援 NNAPI |

---

## 3. 離線辨識管線（核心 — 「幾萬詞」如何做到）

一般分類模型（MobileNet / EfficientNet on ImageNet-1k）**只有 1000 類** → 這就是「只有幾十、幾百個」的來源。
要做到幾萬個，正解是 **開放詞彙（open-vocabulary）影像-文字模型**：

```
相機影格 ──▶ MobileCLIP 影像編碼器 (TFLite, 端上即時, ~10–20ms)
                       │ 產生 512 維影像向量
                       ▼
        與「2 萬名詞文字向量表」做餘弦相似度比對   ◀── 預先離線算好、打包進 App
                       │ 取 Top-1 / Top-5
                       ▼
              英文單字 ──▶ 內建 EN→繁/簡 詞庫 ──▶ 中文
                       │
                       ▼
              U²-Net 去背 ──▶ 去背 PNG 貼紙
```

**關鍵洞見**：
- 端上**只需**：MobileCLIP 影像編碼器 + 文字向量表（2 萬 × 512，int8 ≈ 10MB）+ 單字/翻譯表。
- MobileCLIP **文字編碼器不上機**：只在 PC 端跑一次，把 2 萬名詞編成向量表打包。
- 要支援更多詞，只要換一張更大的向量表，App 程式碼不用動。
- **去背**：U²-Net (u2netp, ~4.7MB) 顯著物體分割 → 去背 PNG。

### 模型清單

| 用途 | 模型 | 端上 / PC | 大小（約） |
|---|---|---|---|
| 即時辨識（影像編碼） | MobileCLIP-S0 image encoder (TFLite) | 端上 | 小 |
| 文字向量預算（離線） | MobileCLIP text encoder | 僅 PC 一次 | — |
| 去背 | U²-Net (u2netp, TFLite) | 端上 | ~4.7MB |
| 文字向量表 | 2 萬名詞 × 512, int8 | 端上 (assets) | ~10MB |

---

## 4. 資料管線（PC / Python 產生，腳本納入專案 `tools/`）

1. **名詞表**：WordNet 名詞 ∩ 具體實體 (physical_entity) ∩ 詞頻表 ∩ ImageNet-21k 標籤 → 精選 ~2 萬「拍得出來」的名詞。
2. **翻譯表**：OPUS-MT en→zh 對 2 萬詞批次翻譯一次 → zh-CN；再用 **OpenCC** 轉 zh-TW → 繁簡兩份。
3. **文字向量表**：MobileCLIP 文字編碼器把 2 萬詞編成向量 → int8 量化 → `.bin` + 索引。
4. 輸出 `words.tsv`（en / zh-TW / zh-CN）+ `text_embeddings.bin` 進 App 的 `assets/`。

---

## 5. 專案結構

```
capwords/
├─ docs/
│  └─ PLAN.md                       # 本文件
├─ android/                         # Android App（Phase 1 起）
│  ├─ settings.gradle.kts
│  ├─ build.gradle.kts
│  ├─ gradle.properties
│  └─ app/
│     ├─ build.gradle.kts
│     └─ src/main/
│        ├─ AndroidManifest.xml
│        ├─ assets/                 # 模型 + 向量表 + 詞庫（Phase 3 放入）
│        ├─ res/                    # 主題、字串、圖示
│        └─ java/com/capwords/
│           ├─ MainActivity.kt
│           ├─ ui/                  # camera / capture / recognize / gallery / components / theme
│           ├─ ml/                  # ClipImageEncoder, NounMatcher, U2NetSegmenter, Recognizer 介面
│           ├─ data/               # Room: WordEntity, WordDao, AppDatabase, Repository
│           └─ tts/                 # SpeechHelper
└─ tools/                           # Python 資料管線（Phase 2）
   ├─ build_wordlist.py
   ├─ translate_words.py            # OPUS-MT + OpenCC
   ├─ encode_text.py                # MobileCLIP text encoder → bin
   └─ convert_models.py             # PyTorch → TFLite（image enc + u2net）
```

---

## 6. 五個畫面（對齊影片流程）

| 畫面 | 內容 |
|---|---|
| `CameraScreen` | CameraX 預覽、對焦框、頂部日期、彩虹快門鍵、相簿縮圖；即時辨識結果浮在框上 |
| `CaptureScreen` | 凍結影格 + U²-Net 去背預覽，裁切 / ✓ / ✕ 工具列 |
| `RecognizeScreen` | 去背貼紙 + 英文 + 中文 + 喇叭 TTS，「結果不如預期？點一下調整」可改選 Top-5 |
| `WordCard` | 完整單字卡（貼紙、雙語、發音） |
| `GalleryScreen` | Room 撈出，依日期分組瀑布流，顯示貼紙 + 單字 + 「N Words」 |

---

## 7. 分階段交付（里程碑）

- **Phase 1 — 可跑的骨架**
  Compose 專案、5 畫面導覽、CameraX 預覽、Room、TTS。
  辨識先用**佔位 stub**（或 ML Kit Image Labeling 暫代）→ 全流程點得通、跑得起來、可 demo。
- **Phase 2 — 資料管線**
  Python 腳本產生 2 萬名詞 + 繁簡翻譯 + 文字向量表。
- **Phase 3 — 接真模型**
  轉換並接上 MobileCLIP 影像編碼器（即時辨識）+ U²-Net（去背），換掉 stub。
- **Phase 4 — 打磨**
  動畫、瀑布流、貼紙白邊、Top-5 調整、效能（GPU delegate、節流）。

---

## 8. 風險與誠實說明

- 此開發環境**不一定能下載並轉換 MobileCLIP / U²-Net**（需網路 + PyTorch + 算力）。
  → 全部 App 原始碼 + 資料管線腳本會寫好，並標清楚哪幾步需在有網路 / Python / Android Studio 的機器執行。
- **Phase 1 先用 stub / ML Kit 暫代辨識**，確保 App 立刻能跑；模型轉好後以一個設定旗標切換成 MobileCLIP。
- **準確度 vs 廣度**：開放詞彙涵蓋幾萬詞，但精準命中略低於鎖定 1000 類的模型；故採「精選具體名詞」而非整本字典（剔除抽象詞）。
- **APK 體積估計**：模型 + 向量表 + 翻譯表約 **30–60MB**（可接受）。

---

## 9. 進度追蹤

| Phase | 狀態 | 備註 |
|---|---|---|
| 計畫文件 | ✅ 完成 | 本文件 |
| Phase 1 骨架 | ✅ 完成並驗證 | 五畫面 + CameraX + Room + TTS；`./gradlew assembleDebug` **BUILD SUCCESSFUL**（zero warnings），產出 app-debug.apk |
| Phase 2 資料管線 | ✅ 完成並執行 | `tools/` 已實際跑出 `clip_words.txt`(2萬) + `words.tsv`(繁簡,99%) + `text_embeddings.bin`(20000×512) |
| Phase 3 接真模型 | ✅ 完成並驗證 | MobileCLIP-S0 影像編碼器走 **ONNX Runtime**（非 TFLite）；端到端實測命中（donut/cup）；`assembleDebug` 成功打包 213MB APK |
| Phase 4 打磨 | 🟡 部分 | ✅ APK 體積優化完成（213MB→97MB）；待辦：動畫、瀑布流、U²-Net 離線去背 |

### APK 體積優化（2026-06-25）：213MB → 97MB（−54%）
- **只保留 arm64-v8a ABI**（`abiFilters`）：丟掉 x86/x86_64/armeabi-v7a 的 ONNX Runtime 原生庫，約 −53MB。
- **文字向量表改 int8**（float32 scale + N×512 int8）：41MB → 10MB（壓縮後 6.9MB），端到端實測仍命中 donut（dequant 誤差 0.0024）。`ClipRecognizer` 載入時還原成 float。
- **assets 改為壓縮打包**（移除 noCompress）。
- **未採用的優化（皆實測失敗）**：
  - int8 **動態**量化（11.9MB）→ 辨識崩壞（donut→curtain）。
  - int8 **靜態**量化（12.5MB，用影片畫面做 in-domain 校準 + per-channel QDQ）→ 與 fp32 **cosine 僅 0.14**，等同壞掉（donut→mason jar、mug→fridge）。
  - fp16（onnxconverter-common，含 keep_io_types / op_block_list / disable_shape_infer 共 4 變體）→ ONNX Runtime 1.20 初始化型別不符，全部載入失敗。
  - 根因：MobileCLIP-S0 影像編碼器為 FastViT 系（重參數化 conv），activation 動態範圍對 int8 太敏感；fp16 為工具相容性問題。
  - **決定**：`mobileclip_image.onnx` 維持 fp32（42MB，最大單一資產）。**接受 97MB 全離線版**（不走首次下載/PAD）。未來若要更小，正解是「首次啟動下載模型」把 base APK 降到 ~55MB，而非量化此模型。

### Phase 3 重大決策與驗證（2026-06-25）
- **辨識廣度路線確定**：開放詞彙 = MobileCLIP 影像向量 ⨉ 2 萬名詞文字向量表（餘弦最近鄰）。換更大的表即可支援更多詞，App 不改碼。
- **詞表品質**：WordNet 具體名詞（lexname=artifact/animal/plant/food/object/body/substance）+ 主要詞義過濾 + 小停用詞表 → 乾淨可拍攝的 2 萬名詞。
- **翻譯改用 ECDICT 開源詞典**（非 NMT，NMT 對單字會重複/選錯詞義）：99% 覆蓋、繁簡雙份（OpenCC s2twp）。
- **改走 ONNX Runtime（非 TFLite）**：MobileCLIP-S0 影像編碼器轉 ONNX 後 **ONNX==PyTorch cosine 1.000000**；但 onnx2tf→TFLite 對此圖反覆失敗，故直接在 Android 跑已驗證的 ONNX，更可靠。`ClipRecognizer` 改為 256×256 / 0..1 / NCHW。
- **端到端實測（用影片畫面）**：物體填滿畫面時，frame_002/004 甜甜圈 → `donut`(top, 0.29)、frame_019 紅杯 → `cup/coffee cup`。確認「離線、幾萬詞、即時辨識」成立。
- **產品改進**：辨識改跑「去背後的主體」而非整張畫面（甜甜圈放大盤上時，整張會誤判成 plate）。
- **APK**：debug 213MB（含 4 種 ABI 的 ONNX Runtime + 86MB 模型資產）。Release 可用 ABI split（只留 arm64）+ fp16 模型 + int8 向量表大幅縮小。

### Phase 1 驗證結果（2026-06-25）
- 工具鏈：本機 Android SDK + Gradle（wrapper 釘 8.9，相容 AGP 8.5.2）。
- `:app:compileDebugKotlin` → 成功；`:app:assembleDebug` → **BUILD SUCCESSFUL**，產出 `app/build/outputs/apk/debug/app-debug.apk`（debug 約 86MB，未裁切原生庫；release 經 minify + ABI split 會大幅縮小）。
- 修正：ML Kit 主體分割改用 `com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1`；關閉 configuration-cache；清掉 LocalLifecycleOwner / VolumeUp 兩個 deprecation。
