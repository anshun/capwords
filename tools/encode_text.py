#!/usr/bin/env python3
"""Pre-compute MobileCLIP text embeddings for the noun list.

Each noun is embedded with a prompt template ("a photo of a {word}"), L2-normalized,
and written row-aligned to clip_words.txt as float32. On device we compute one
image embedding and take the nearest neighbour in this table.

Output: text_embeddings.bin  (N*D little-endian float32, L2-normalized)

Run:
    python encode_text.py \
        --words ../android/app/src/main/assets/clip_words.txt \
        --out ../android/app/src/main/assets/text_embeddings.bin \
        --model mobileclip_s0
"""
import argparse
import struct


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--words", default="clip_words.txt")
    ap.add_argument("--out", default="text_embeddings.bin")
    ap.add_argument("--model", default="mobileclip_s0")
    ap.add_argument("--ckpt", default="checkpoints/mobileclip_s0.pt")
    ap.add_argument("--batch", type=int, default=128)
    ap.add_argument("--device", default="auto", help="auto | cpu | mps | cuda")
    args = ap.parse_args()

    import torch
    import mobileclip  # pip install git+https://github.com/apple/ml-mobileclip.git

    if args.device == "auto":
        device = (
            "cuda" if torch.cuda.is_available()
            else "mps" if torch.backends.mps.is_available()
            else "cpu"
        )
    else:
        device = args.device
    print(f"encoding on device: {device}")

    model, _, _ = mobileclip.create_model_and_transforms(args.model, pretrained=args.ckpt)
    tokenizer = mobileclip.get_tokenizer(args.model)
    model.eval().to(device)

    with open(args.words, encoding="utf-8") as f:
        words = [w.strip() for w in f if w.strip()]

    prompts = [f"a photo of a {w}" for w in words]

    import numpy as np

    rows = []
    with torch.no_grad():
        for i in range(0, len(prompts), args.batch):
            batch = prompts[i : i + args.batch]
            tokens = tokenizer(batch).to(device)
            feats = model.encode_text(tokens)
            feats = feats / feats.norm(dim=-1, keepdim=True)
            rows.append(feats.cpu().float().numpy())
            print(f"{min(i + args.batch, len(prompts))}/{len(prompts)}")
    emb = np.concatenate(rows, axis=0)  # [N, dim], L2-normalized
    dim = emb.shape[1]

    # int8 quantization with a single global scale (~4x smaller, negligible loss).
    # File layout: float32 scale (LE) + N*dim int8. Device dequant: q * scale.
    scale = float(np.abs(emb).max()) / 127.0
    q = np.round(emb / scale).clip(-127, 127).astype(np.int8)
    with open(args.out, "wb") as out:
        out.write(struct.pack("<f", scale))
        out.write(q.tobytes())
    print(f"Wrote int8 embeddings (N={emb.shape[0]}, dim={dim}, scale={scale:.6g}) -> {args.out}")


if __name__ == "__main__":
    main()
