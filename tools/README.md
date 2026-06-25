# tools/ — offline data & model pipeline

Run these once on a machine with network + Python 3.10+ (a GPU helps for conversion).
They produce the files the Android app loads from `assets/`.

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
pip install git+https://github.com/apple/ml-mobileclip.git   # MobileCLIP

# 1) Build the ~20k concrete-noun list
python build_wordlist.py --top 20000 \
    --out ../android/app/src/main/assets/clip_words.txt

# 2) Translate to zh-TW + zh-CN
python translate_words.py \
    --words ../android/app/src/main/assets/clip_words.txt \
    --out   ../android/app/src/main/assets/words.tsv

# 3) Pre-compute MobileCLIP text embeddings (download a checkpoint first)
python encode_text.py \
    --words ../android/app/src/main/assets/clip_words.txt \
    --out   ../android/app/src/main/assets/text_embeddings.bin \
    --model mobileclip_s0 --ckpt checkpoints/mobileclip_s0.pt

# 4) Convert the image encoder (and optionally U2Net) to TFLite
python convert_models.py --target clip  --ckpt checkpoints/mobileclip_s0.pt
python convert_models.py --target u2net --ckpt checkpoints/u2netp.pth
# copy converted/*_float32.tflite -> assets/mobileclip_image.tflite (+ u2netp.tflite)
```

When `mobileclip_image.tflite`, `clip_words.txt`, and `text_embeddings.bin` are all
present in `assets/`, the app automatically switches from the ML Kit fallback to
MobileCLIP open-vocabulary recognition — no code change required
(see `ClipRecognizer.assetsAvailable`).

## Model sources
- MobileCLIP — https://github.com/apple/ml-mobileclip (image encoder runs on device;
  text encoder only used here to build the embedding table)
- U²-Net (u2netp) — https://github.com/xuebinqin/U-2-Net
- OPUS-MT en→zh — https://huggingface.co/Helsinki-NLP/opus-mt-en-zh
- OpenCC — Simplified⇄Traditional conversion

## Size budget (≈20k words)
- `text_embeddings.bin`: 20000 × 512 × 4 B ≈ 40 MB float32 (≈10 MB if int8-quantized)
- `mobileclip_image.tflite`: small (S0 image encoder)
- `u2netp.tflite`: ≈4.7 MB
- `words.tsv`: a few MB
