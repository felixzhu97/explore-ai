#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODELS_DIR="${ROOT_DIR}/models"
mkdir -p "${MODELS_DIR}" "${MODELS_DIR}/tessdata" "${MODELS_DIR}/blip_tokenizer"

echo "Downloading YOLOv8n ONNX..."
curl -fsSL -L -o "${MODELS_DIR}/yolov8n.onnx" \
  "https://huggingface.co/Kalray/yolov8/resolve/main/yolov8n.onnx"

echo "Downloading BLIP ONNX models from HuggingFace..."
curl -fsSL -L -o "${MODELS_DIR}/blip_vision_model.onnx" \
  "https://huggingface.co/onnx-community/Salesforce_blip-image-captioning-base/resolve/main/split_0.onnx"
curl -fsSL -L -o "${MODELS_DIR}/blip_text_decoder.onnx" \
  "https://huggingface.co/onnx-community/Salesforce_blip-image-captioning-base/resolve/main/split_1.onnx"
curl -fsSL -L -o "${MODELS_DIR}/blip_tokenizer/vocab.txt" \
  "https://huggingface.co/Salesforce/blip-image-captioning-base/resolve/main/vocab.txt"

echo "Downloading Tesseract tessdata (eng)..."
curl -fsSL -o "${MODELS_DIR}/tessdata/eng.traineddata" \
  "https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata"
curl -fsSL -o "${MODELS_DIR}/tessdata/chi_sim.traineddata" \
  "https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata"

echo "Vision models downloaded to ${MODELS_DIR}"
echo "Set VISION_MODELS_READY=true for integration tests."
