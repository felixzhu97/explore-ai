package com.ai.vision.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vision")
public class VisionProperties {

	private YoloConfig yolo = new YoloConfig();
	private BlipConfig blip = new BlipConfig();
	private OcrConfig ocr = new OcrConfig();
	private StableDiffusionConfig stableDiffusion = new StableDiffusionConfig();

	public YoloConfig getYolo() {
		return yolo;
	}

	public void setYolo(YoloConfig yolo) {
		this.yolo = yolo;
	}

	public BlipConfig getBlip() {
		return blip;
	}

	public void setBlip(BlipConfig blip) {
		this.blip = blip;
	}

	public OcrConfig getOcr() {
		return ocr;
	}

	public void setOcr(OcrConfig ocr) {
		this.ocr = ocr;
	}

	public StableDiffusionConfig getStableDiffusion() {
		return stableDiffusion;
	}

	public void setStableDiffusion(StableDiffusionConfig stableDiffusion) {
		this.stableDiffusion = stableDiffusion;
	}

	public static class YoloConfig {
		private String modelPath = "/models/yolo/yolov8n.onnx";
		private float confidence = 0.5f;
		private String device = "cpu";

		public String getModelPath() {
			return modelPath;
		}

		public void setModelPath(String modelPath) {
			this.modelPath = modelPath;
		}

		public float getConfidence() {
			return confidence;
		}

		public void setConfidence(float confidence) {
			this.confidence = confidence;
		}

		public String getDevice() {
			return device;
		}

		public void setDevice(String device) {
			this.device = device;
		}
	}

	public static class BlipConfig {
		private String modelPath = "/models/blip";
		private String modelName = "Salesforce/blip-image-captioning-base";
		private int maxLength = 100;

		public String getModelPath() {
			return modelPath;
		}

		public void setModelPath(String modelPath) {
			this.modelPath = modelPath;
		}

		public String getModelName() {
			return modelName;
		}

		public void setModelName(String modelName) {
			this.modelName = modelName;
		}

		public int getMaxLength() {
			return maxLength;
		}

		public void setMaxLength(int maxLength) {
			this.maxLength = maxLength;
		}
	}

	public static class OcrConfig {
		private String modelPath = "/models/ocr";
		private String language = "eng+chi_sim";
		private boolean enableTableDetection = true;

		public String getModelPath() {
			return modelPath;
		}

		public void setModelPath(String modelPath) {
			this.modelPath = modelPath;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public boolean isEnableTableDetection() {
			return enableTableDetection;
		}

		public void setEnableTableDetection(boolean enableTableDetection) {
			this.enableTableDetection = enableTableDetection;
		}
	}

	public static class StableDiffusionConfig {
		private String modelPath = "/models/stable-diffusion";
		private String modelName = "stabilityai/stable-diffusion-2-1";
		private int defaultSteps = 30;
		private int defaultWidth = 512;
		private int defaultHeight = 512;
		private String cacheDir = "/tmp/sd-cache";

		public String getModelPath() {
			return modelPath;
		}

		public void setModelPath(String modelPath) {
			this.modelPath = modelPath;
		}

		public String getModelName() {
			return modelName;
		}

		public void setModelName(String modelName) {
			this.modelName = modelName;
		}

		public int getDefaultSteps() {
			return defaultSteps;
		}

		public void setDefaultSteps(int defaultSteps) {
			this.defaultSteps = defaultSteps;
		}

		public int getDefaultWidth() {
			return defaultWidth;
		}

		public void setDefaultWidth(int defaultWidth) {
			this.defaultWidth = defaultWidth;
		}

		public int getDefaultHeight() {
			return defaultHeight;
		}

		public void setDefaultHeight(int defaultHeight) {
			this.defaultHeight = defaultHeight;
		}

		public String getCacheDir() {
			return cacheDir;
		}

		public void setCacheDir(String cacheDir) {
			this.cacheDir = cacheDir;
		}
	}
}
