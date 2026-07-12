package com.ai.vision.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vision")
public class VisionModelProperties {

    private String modelsDir = "models";
    private Detect detect = new Detect();
    private Caption caption = new Caption();
    private Ocr ocr = new Ocr();

    public String getModelsDir() {
        return modelsDir;
    }

    public void setModelsDir(String modelsDir) {
        this.modelsDir = modelsDir;
    }

    public Detect getDetect() {
        return detect;
    }

    public void setDetect(Detect detect) {
        this.detect = detect;
    }

    public Caption getCaption() {
        return caption;
    }

    public void setCaption(Caption caption) {
        this.caption = caption;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public void setOcr(Ocr ocr) {
        this.ocr = ocr;
    }

    public static class Detect {
        private String onnxPath = "models/yolov8n.onnx";
        private float confidenceThreshold = 0.5f;
        private float nmsThreshold = 0.45f;
        private int inputSize = 640;

        public String getOnnxPath() {
            return onnxPath;
        }

        public void setOnnxPath(String onnxPath) {
            this.onnxPath = onnxPath;
        }

        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(float confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        public float getNmsThreshold() {
            return nmsThreshold;
        }

        public void setNmsThreshold(float nmsThreshold) {
            this.nmsThreshold = nmsThreshold;
        }

        public int getInputSize() {
            return inputSize;
        }

        public void setInputSize(int inputSize) {
            this.inputSize = inputSize;
        }
    }

    public static class Caption {
        private String visionOnnx = "models/blip_vision_model.onnx";
        private String decoderOnnx = "models/blip_text_decoder.onnx";
        private String tokenizerPath = "models/blip_tokenizer";
        private int maxLength = 50;

        public String getVisionOnnx() {
            return visionOnnx;
        }

        public void setVisionOnnx(String visionOnnx) {
            this.visionOnnx = visionOnnx;
        }

        public String getDecoderOnnx() {
            return decoderOnnx;
        }

        public void setDecoderOnnx(String decoderOnnx) {
            this.decoderOnnx = decoderOnnx;
        }

        public String getTokenizerPath() {
            return tokenizerPath;
        }

        public void setTokenizerPath(String tokenizerPath) {
            this.tokenizerPath = tokenizerPath;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }

    public static class Ocr {
        private String tessdataPath = "models/tessdata";
        private String languages = "eng+chi_sim";
        private int pageSegMode = 3;

        public String getTessdataPath() {
            return tessdataPath;
        }

        public void setTessdataPath(String tessdataPath) {
            this.tessdataPath = tessdataPath;
        }

        public String getLanguages() {
            return languages;
        }

        public void setLanguages(String languages) {
            this.languages = languages;
        }

        public int getPageSegMode() {
            return pageSegMode;
        }

        public void setPageSegMode(int pageSegMode) {
            this.pageSegMode = pageSegMode;
        }
    }
}
