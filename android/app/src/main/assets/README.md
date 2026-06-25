# assets/

Phase 3 on-device model + data files. When all three CLIP files are present,
`ClipRecognizer.assetsAvailable()` returns true and the app uses MobileCLIP
open-vocabulary recognition (~20k nouns); otherwise it falls back to the ML Kit
labeler (~400 classes).

| File | Produced by | Purpose |
|------|-------------|---------|
| `mobileclip_image.onnx` | `tools/convert_models.py` (ONNX export) | MobileCLIP-S0 image encoder, input `image` `[1,3,256,256]` f32 NCHW (0..1), output `[1,512]` L2-normalized. Runs on-device via **ONNX Runtime Mobile**. |
| `clip_words.txt` | `tools/build_wordlist.py` | N English nouns, one per line (row-aligned to embeddings) |
| `text_embeddings.bin` | `tools/encode_text.py` | int8 table: float32 `scale` (LE) + `N*512` int8; device dequant = `q * scale` (≈4× smaller than float32, negligible loss) |
| `words.tsv` | `tools/translate_words.py` | `english \t zh-TW \t zh-CN`, one per line (translation table) |
| `u2netp.onnx` | rembg release (or `tools/convert_models.py --target u2net`) | U²-Net salient-object segmentation, input `input.1` `[1,3,320,320]`; fully-offline subject cut-out. When present, used instead of ML Kit. |

## Why ONNX Runtime (not TFLite)
The MobileCLIP-S0 image encoder converts to ONNX cleanly and ONNX==PyTorch
numerically (cosine 1.000000), but onnx2tf conversion is brittle for this graph.
Running the validated ONNX directly via ONNX Runtime Mobile is the reliable path
and needs no conversion. Text embeddings come from the *same* MobileCLIP model so
image and text live in one shared space.

## Preprocessing (must match exactly)
Resize to 256×256, scale to 0..1 (ToTensor), **no** ImageNet mean/std, NCHW planar.
