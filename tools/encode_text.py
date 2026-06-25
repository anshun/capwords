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
    args = ap.parse_args()

    import torch
    import mobileclip  # pip install git+https://github.com/apple/ml-mobileclip.git

    model, _, _ = mobileclip.create_model_and_transforms(args.model, pretrained=args.ckpt)
    tokenizer = mobileclip.get_tokenizer(args.model)
    model.eval()

    with open(args.words, encoding="utf-8") as f:
        words = [w.strip() for w in f if w.strip()]

    prompts = [f"a photo of a {w}" for w in words]

    with open(args.out, "wb") as out, torch.no_grad():
        dim = None
        for i in range(0, len(prompts), args.batch):
            batch = prompts[i : i + args.batch]
            tokens = tokenizer(batch)
            feats = model.encode_text(tokens)
            feats = feats / feats.norm(dim=-1, keepdim=True)
            arr = feats.cpu().float().numpy()
            if dim is None:
                dim = arr.shape[1]
            for row in arr:
                out.write(struct.pack(f"<{dim}f", *row))
            print(f"{min(i + args.batch, len(prompts))}/{len(prompts)}")
    print(f"Wrote embeddings (dim={dim}) -> {args.out}")


if __name__ == "__main__":
    main()
