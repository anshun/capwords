#!/usr/bin/env python3
"""Translate the English noun list to Chinese (Simplified + Traditional).

Single bare nouns are unreliable through NMT (it repeats / picks verb senses), so
we use ECDICT — an open English->Chinese dictionary — for clean glosses, then
OpenCC (s2twp) to derive Traditional from Simplified.

ECDICT: https://github.com/skywind3000/ECDICT  (download ecdict.csv)

Output: words.tsv  (english \t zh-TW \t zh-CN)

Run:
    python translate_words.py \
        --words   ../android/app/src/main/assets/clip_words.txt \
        --ecdict  data/ecdict.csv \
        --out     ../android/app/src/main/assets/words.tsv
"""
import argparse
import csv
import re

CJK = re.compile(r"[一-鿿]")

# A few common everyday words whose ECDICT primary gloss is a technical/rare sense
# (e.g. "donut" resolves to a cyclotron part). Stored Simplified; Traditional is
# derived via OpenCC like everything else.
OVERRIDES = {
    "donut": "甜甜圈",
    "doughnut": "甜甜圈",
    "mug": "马克杯",
    "fries": "薯条",
}
# Leading "[网络] ", "[计] ", and POS markers like "n. ", "vt. ", "un. ".
POS_PREFIX = re.compile(r"^\s*(?:\[[^\]]*\]\s*)?(?:[a-zA-Z]{1,5}\.\s*)?")
NOUN_LINE = re.compile(r"^\s*(?:\[[^\]]*\]\s*)?n\.")
GLOSS_SEP = re.compile(r"[,，;；、]")


def pick_chinese(translation: str) -> str | None:
    """Extract one clean Chinese noun gloss from an ECDICT translation cell."""
    # ECDICT stores line breaks inside the cell as the literal 2 chars "\n".
    lines = [ln.strip() for ln in translation.split("\\n") if ln.strip()]
    if not lines:
        return None
    # Prefer the noun line; otherwise the first line.
    line = next((ln for ln in lines if NOUN_LINE.match(ln)), lines[0])
    line = POS_PREFIX.sub("", line)
    # First gloss that actually contains Chinese (senses are relevance-ordered).
    for gloss in GLOSS_SEP.split(line):
        gloss = gloss.strip()
        if CJK.search(gloss):
            return gloss
    return None


def load_ecdict(path: str) -> dict[str, str]:
    table: dict[str, str] = {}
    with open(path, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            tr = row.get("translation") or ""
            if not tr:
                continue
            zh = pick_chinese(tr)
            if zh:
                table[row["word"].strip().lower()] = zh
    return table


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--words", default="clip_words.txt")
    ap.add_argument("--ecdict", default="data/ecdict.csv")
    ap.add_argument("--out", default="words.tsv")
    args = ap.parse_args()

    from opencc import OpenCC

    cc = OpenCC("s2twp")  # Simplified -> Traditional (Taiwan phrasing)
    print("loading ECDICT…")
    ecdict = load_ecdict(args.ecdict)
    print(f"ECDICT usable entries: {len(ecdict)}")

    with open(args.words, encoding="utf-8") as f:
        words = [w.strip() for w in f if w.strip()]

    hit = 0
    with open(args.out, "w", encoding="utf-8") as out:
        for w in words:
            cn = OVERRIDES.get(w.lower()) or ecdict.get(w.lower())
            if cn:
                hit += 1
                tw = cc.convert(cn)
            else:
                tw = ""
                cn = ""
            out.write(f"{w}\t{tw}\t{cn}\n")
    print(f"Wrote {len(words)} rows -> {args.out}  (translated {hit}, {hit * 100 // len(words)}% coverage)")


if __name__ == "__main__":
    main()
