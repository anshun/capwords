#!/usr/bin/env python3
"""End-to-end check of the shipped recognition assets.

Mirrors the Android app exactly: straight-resize the image to 256x256, scale to
0..1 (NCHW), run mobileclip_image.onnx via ONNX Runtime, and nearest-neighbour
against text_embeddings.bin. Prints the top-k words per image.

    python validate_pipeline.py img1.png img2.png ...
"""
import sys
import numpy as np
import onnxruntime as ort
from PIL import Image

ASSETS = "../android/app/src/main/assets"


def load_table():
    words = open(f"{ASSETS}/clip_words.txt", encoding="utf-8").read().split("\n")
    words = [w for w in words if w]
    emb = np.fromfile(f"{ASSETS}/text_embeddings.bin", dtype="<f4")
    dim = emb.size // len(words)
    return words, emb.reshape(len(words), dim)


def embed_image(sess, path):
    img = Image.open(path).convert("RGB").resize((256, 256))  # straight resize like the app
    x = np.asarray(img, dtype=np.float32) / 255.0             # HWC 0..1
    x = np.transpose(x, (2, 0, 1))[None]                      # NCHW
    out = sess.run(None, {"image": x})[0][0]
    return out / (np.linalg.norm(out) + 1e-8)


def main():
    words, table = load_table()
    sess = ort.InferenceSession(f"{ASSETS}/mobileclip_image.onnx", providers=["CPUExecutionProvider"])
    for path in sys.argv[1:]:
        v = embed_image(sess, path)
        scores = table @ v
        top = np.argsort(-scores)[:5]
        print(f"\n{path}")
        for i in top:
            print(f"  {scores[i]:.3f}  {words[i]}")


if __name__ == "__main__":
    main()
