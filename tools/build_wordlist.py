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

# WordNet lexicographer files that correspond to things you can photograph.
CONCRETE_LEXNAMES = {
    "noun.artifact",    # man-made objects (cup, chair, phone)
    "noun.animal",      # animals
    "noun.plant",       # plants, flowers, trees
    "noun.food",        # food & drink
    "noun.object",      # natural objects (rock, star, beach)
    "noun.body",        # body parts
    "noun.substance",   # materials (water, sand, metal)
}

# Senses are returned most-common first; require the *primary* noun sense to be
# concrete so words whose dominant meaning is abstract (two, need, may, work) are
# dropped even though they carry some buried concrete sense.
PRIMARY_SENSES = 1

# Short, common words that pass the lexname test via an obscure concrete sense
# (down=feathers, must=grape-juice, john=toilet) but read as noise on a label.
# Body parts and real objects are deliberately NOT here.
STOPWORDS = {
    "way", "out", "come", "must", "after", "world", "still", "two", "three",
    "four", "year", "old", "young", "big", "working", "means", "part", "group",
    "system", "life", "point", "local", "take-up", "set-back", "a-line",
    "life-of-man", "so-and-so", "down", "up", "off", "over", "under", "shit",
    "john", "being", "thing", "lot", "kind", "sort", "type", "number", "one",
    "can-do", "have-not", "has-been", "know-all", "know-it-all",
}


def concrete_nouns() -> set[str]:
    import nltk
    nltk.download("wordnet", quiet=True)
    nltk.download("omw-1.4", quiet=True)
    from nltk.corpus import wordnet as wn

    keep: set[str] = set()
    for lemma_name in wn.all_lemma_names(pos=wn.NOUN):
        word = lemma_name.replace("_", " ").lower()
        if not TOKEN_RE.match(word) or len(word) > 24:
            continue
        if word in STOPWORDS:
            continue
        senses = wn.synsets(lemma_name, pos=wn.NOUN)[:PRIMARY_SENSES]
        if any(s.lexname() in CONCRETE_LEXNAMES for s in senses):
            keep.add(word)
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
