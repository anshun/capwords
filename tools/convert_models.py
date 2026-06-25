#!/usr/bin/env python3
"""Convert PyTorch models to TFLite for on-device inference.

Pipeline: PyTorch -> ONNX -> TF SavedModel (onnx2tf) -> TFLite.

Produces:
    mobileclip_image.tflite   image encoder, input [1,224,224,3] f32 (NHWC), output [1,D]
    u2netp.tflite             (optional) bundled offline segmentation

Run:
    python convert_models.py --target clip  --ckpt checkpoints/mobileclip_s0.pt
    python convert_models.py --target u2net --ckpt checkpoints/u2netp.pth
"""
import argparse
import subprocess
import sys


def export_clip_image_encoder(ckpt: str, model_name: str, onnx_path: str) -> None:
    import torch
    import mobileclip

    model, _, _ = mobileclip.create_model_and_transforms(model_name, pretrained=ckpt)
    model.eval()

    class ImageEncoder(torch.nn.Module):
        def __init__(self, m):
            super().__init__()
            self.m = m

        def forward(self, x):  # x: [B,3,224,224]
            feats = self.m.encode_image(x)
            return feats / feats.norm(dim=-1, keepdim=True)

    wrapper = ImageEncoder(model)
    dummy = torch.randn(1, 3, 224, 224)
    torch.onnx.export(
        wrapper, dummy, onnx_path,
        input_names=["image"], output_names=["embedding"],
        opset_version=17, dynamic_axes=None,
    )
    print(f"ONNX -> {onnx_path}")


def export_u2net(ckpt: str, onnx_path: str) -> None:
    import torch
    # Expects the U2NETP architecture from https://github.com/xuebinqin/U-2-Net
    from model import U2NETP  # place u2net model.py on PYTHONPATH

    net = U2NETP(3, 1)
    net.load_state_dict(torch.load(ckpt, map_location="cpu"))
    net.eval()
    dummy = torch.randn(1, 3, 320, 320)
    torch.onnx.export(
        net, dummy, onnx_path,
        input_names=["image"], output_names=["mask"],
        opset_version=17,
    )
    print(f"ONNX -> {onnx_path}")


def onnx_to_tflite(onnx_path: str, out_dir: str) -> None:
    # onnx2tf emits NHWC TFLite and handles the NCHW->NHWC transpose.
    subprocess.check_call([sys.executable, "-m", "onnx2tf", "-i", onnx_path, "-o", out_dir])
    print(f"onnx2tf SavedModel + TFLite -> {out_dir}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--target", choices=["clip", "u2net"], required=True)
    ap.add_argument("--ckpt", required=True)
    ap.add_argument("--model", default="mobileclip_s0")
    ap.add_argument("--out_dir", default="converted")
    args = ap.parse_args()

    if args.target == "clip":
        onnx = "mobileclip_image.onnx"
        export_clip_image_encoder(args.ckpt, args.model, onnx)
    else:
        onnx = "u2netp.onnx"
        export_u2net(args.ckpt, onnx)

    onnx_to_tflite(onnx, args.out_dir)
    print("Done. Copy the *_float32.tflite into android/app/src/main/assets/ and rename per assets/README.md")


if __name__ == "__main__":
    main()
