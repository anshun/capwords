# assets/

Phase 3 drops the on-device model + data files here. Until they exist, the app
falls back to the ML Kit recognizer (~400 classes) and the seed translation map.

Expected files (produced by `tools/`):

| File | Produced by | Purpose |
|------|-------------|---------|
| `mobileclip_image.tflite` | `tools/convert_models.py` | MobileCLIP image encoder, input `[1,224,224,3]` f32, output `[1,D]` |
| `clip_words.txt` | `tools/build_wordlist.py` | N English nouns, one per line (row-aligned to embeddings) |
| `text_embeddings.bin` | `tools/encode_text.py` | `N*D` little-endian float32, L2-normalized |
| `words.tsv` | `tools/translate_words.py` | `english \t zh-TW \t zh-CN`, one per line |
| `u2netp.tflite` | `tools/convert_models.py` | (optional) bundled offline segmentation |

`ClipRecognizer.assetsAvailable()` checks for the first three; when present the app
automatically uses MobileCLIP instead of the fallback — no code change needed.
