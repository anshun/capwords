#!/usr/bin/env python3
"""Build the curated ~20k concrete-noun list for open-vocabulary recognition.

Strategy: take WordNet noun lemmas that are hyponyms of `physical_entity`
(i.e. things you can photograph), keep single-token everyday words, rank by
corpus frequency, and cut to TOP_N.

Output: clip_words.txt (one English noun per line, frequency-ranked).

Run:
    python build_wordlist.py --top 20000 --out ../android/app/src/main/assets/clip_words.txt
"""
import argparse
import re

TOKEN_RE = re.compile(r"^[a-z][a-z\- ]+[a-z]$")


def concrete_nouns() -> set[str]:
    import nltk
    nltk.download("wordnet", quiet=True)
    nltk.download("omw-1.4", quiet=True)
    from nltk.corpus import wordnet as wn

    physical = wn.synset("physical_entity.n.01")
    keep: set[str] = set()
    # Breadth-first over all hyponyms of physical_entity.
    stack = [physical]
    seen = set()
    while stack:
        syn = stack.pop()
        if syn.name() in seen:
            continue
        seen.add(syn.name())
        for lemma in syn.lemmas():
            word = lemma.name().replace("_", " ").lower()
            if TOKEN_RE.match(word) and len(word) <= 24:
                keep.add(word)
        stack.extend(syn.hyponyms())
    return keep


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--top", type=int, default=20000)
    ap.add_argument("--out", default="clip_words.txt")
    args = ap.parse_args()

    from wordfreq import word_frequency, zipf_frequency

    nouns = concrete_nouns()
    print(f"WordNet concrete nouns: {len(nouns)}")

    # Rank by frequency; drop ultra-rare words that the encoder rarely needs.
    ranked = sorted(nouns, key=lambda w: word_frequency(w, "en"), reverse=True)
    ranked = [w for w in ranked if zipf_frequency(w, "en") > 1.5]
    ranked = ranked[: args.top]

    with open(args.out, "w", encoding="utf-8") as f:
        for w in ranked:
            f.write(w + "\n")
    print(f"Wrote {len(ranked)} words -> {args.out}")


if __name__ == "__main__":
    main()
