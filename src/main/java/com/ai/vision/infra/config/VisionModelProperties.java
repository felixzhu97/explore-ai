package com.ai.vision.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code app.vision} for the local detection, captioning, and OCR models. */
@ConfigurationProperties(prefix = "app.vision")
@Getter
@Setter
public class VisionModelProperties {

  private String modelsDir = "models";
  private Detect detect = new Detect();
  private Caption caption = new Caption();
  private Ocr ocr = new Ocr();

  /** YOLO detection settings: ONNX path, confidence and NMS thresholds, and input size. */
  @Getter
  @Setter
  public static class Detect {
    private String onnxPath = "models/yolov8n.onnx";
    private float confidenceThreshold = 0.5f;
    private float nmsThreshold = 0.45f;
    private int inputSize = 640;
  }

  /** BLIP captioning settings: vision and decoder ONNX paths, tokenizer, and max caption length. */
  @Getter
  @Setter
  public static class Caption {
    private String visionOnnx = "models/blip_vision_model.onnx";
    private String decoderOnnx = "models/blip_text_decoder.onnx";
    private String tokenizerPath = "models/blip_tokenizer";
    private int maxLength = 50;
  }

  /** Tesseract OCR settings: tessdata path, language codes, and page segmentation mode. */
  @Getter
  @Setter
  public static class Ocr {
    private String tessdataPath = "models/tessdata";
    private String languages = "eng+chi_sim";
    private int pageSegMode = 3;
  }
}
