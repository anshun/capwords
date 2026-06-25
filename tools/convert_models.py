#!/usr/bin/env python3
"""Export models to ONNX for on-device inference via ONNX Runtime Mobile.

The MobileCLIP-S0 image encoder exports to ONNX cleanly and runs ONNX==PyTorch
(cosine 1.0). We ship the .onnx directly and run it with ONNX Runtime on Android
(onnx2tf/TFLite conversion is brittle for this graph and unnecessary).

Produces:
    mobileclip_image.onnx   image encoder, input "image" [1,3,256,256] f32 NCHW (0..1), output [1,512] L2-normalized
    u2netp.onnx             (optional) salient-object segmentation for offline cut-out

Run:
    python convert_models.py --target clip  --ckpt checkpoints/mobileclip_s0.pt \
        --out ../android/app/src/main/assets/mobileclip_image.onnx
    python convert_models.py --target u2net --ckpt checkpoints/u2netp.pth \
        --out ../android/app/src/main/assets/u2netp.onnx
"""
import argparse


def export_clip_image_encoder(ckpt: str, model_name: str, out: str) -> None:
    import torch
    import mobileclip

    model, _, _ = mobileclip.create_model_and_transforms(model_name, pretrained=ckpt)
    model.eval()

    class ImageEncoder(torch.nn.Module):
        def __init__(self, m):
            super().__init__()
            self.m = m

        def forward(self, x):  # x: [B,3,256,256], values 0..1
            feats = self.m.encode_image(x)
            return feats / feats.norm(dim=-1, keepdim=True)

    dummy = torch.randn(1, 3, 256, 256)  # MobileCLIP-S0 expects 256x256
    torch.onnx.export(
        ImageEncoder(model), dummy, out,
        input_names=["image"], output_names=["embedding"],
        opset_version=17, dynamo=False,
    )
    _check(out)


def export_u2net(ckpt: str, out: str) -> None:
    import torch
    from model import U2NETP  # place u2net model.py (xuebinqin/U-2-Net) on PYTHONPATH

    net = U2NETP(3, 1)
    net.load_state_dict(torch.load(ckpt, map_location="cpu"))
    net.eval()

    class Mask(torch.nn.Module):
        def __init__(self, n):
            super().__init__()
            self.n = n

        def forward(self, x):  # x: [B,3,320,320]
            return torch.sigmoid(self.n(x)[0])

    dummy = torch.randn(1, 3, 320, 320)
    torch.onnx.export(
        Mask(net), dummy, out,
        input_names=["image"], output_names=["mask"],
        opset_version=17, dynamo=False,
    )
    _check(out)


def _check(path: str) -> None:
    import onnx
    m = onnx.load(path)
    onnx.checker.check_model(m)
    print(f"ONNX OK -> {path}  ({len(m.graph.node)} nodes)")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--target", choices=["clip", "u2net"], required=True)
    ap.add_argument("--ckpt", required=True)
    ap.add_argument("--model", default="mobileclip_s0")
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    if args.target == "clip":
        export_clip_image_encoder(args.ckpt, args.model, args.out)
    else:
        export_u2net(args.ckpt, args.out)


if __name__ == "__main__":
    main()
