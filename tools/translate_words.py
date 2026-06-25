#!/usr/bin/env python3
"""Translate the English noun list to Chinese (Simplified + Traditional).

Uses Helsinki-NLP OPUS-MT (en->zh) for Simplified, then OpenCC (s2twp) to derive
Traditional. Single nouns translate reliably; this runs once offline on a PC.

Output: words.tsv  (english \t zh-TW \t zh-CN)

Run:
    python translate_words.py \
        --words ../android/app/src/main/assets/clip_words.txt \
        --out ../android/app/src/main/assets/words.tsv
"""
import argparse


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--words", default="clip_words.txt")
    ap.add_argument("--out", default="words.tsv")
    ap.add_argument("--batch", type=int, default=64)
    args = ap.parse_args()

    from transformers import MarianMTModel, MarianTokenizer
    from opencc import OpenCC

    model_name = "Helsinki-NLP/opus-mt-en-zh"
    tok = MarianTokenizer.from_pretrained(model_name)
    model = MarianMTModel.from_pretrained(model_name)
    cc = OpenCC("s2twp")  # Simplified -> Traditional (Taiwan phrasing)

    with open(args.words, encoding="utf-8") as f:
        words = [w.strip() for w in f if w.strip()]

    rows: list[tuple[str, str, str]] = []
    for i in range(0, len(words), args.batch):
        batch = words[i : i + args.batch]
        enc = tok(batch, return_tensors="pt", padding=True, truncation=True)
        gen = model.generate(**enc, max_length=24)
        zh_cn = [tok.decode(g, skip_special_tokens=True) for g in gen]
        for en, cn in zip(batch, zh_cn):
            tw = cc.convert(cn)
            rows.append((en, tw, cn))
        print(f"{min(i + args.batch, len(words))}/{len(words)}")

    with open(args.out, "w", encoding="utf-8") as f:
        for en, tw, cn in rows:
            f.write(f"{en}\t{tw}\t{cn}\n")
    print(f"Wrote {len(rows)} rows -> {args.out}")


if __name__ == "__main__":
    main()
