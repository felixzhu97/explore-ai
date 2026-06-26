package com.ai.ai.application.usecase;

import java.util.List;

public interface ImageGenerationUseCasePort {
    String generateImage(String prompt);
    String generateImage(String prompt, String model, String quality, int width, int height, int n);
    List<String> getAvailableModels();
    List<String> getAvailableSizes();
    List<String> getAvailableQualities();
}
