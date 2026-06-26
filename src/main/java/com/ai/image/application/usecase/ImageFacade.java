package com.ai.image.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade for image generation operations.
 */
@Service
public class ImageFacade {

    private static final Logger log = LoggerFactory.getLogger(ImageFacade.class);

    private final SpringAiImageGenerationUseCase imageGenerationUseCase;

    public ImageFacade(SpringAiImageGenerationUseCase imageGenerationUseCase) {
        this.imageGenerationUseCase = imageGenerationUseCase;
    }

    /**
     * Generate an image from text prompt.
     */
    public String generateImage(String prompt, String model, String quality, int width, int height, int n) {
        log.info("ImageFacade.generateImage: {}", truncate(prompt));
        return imageGenerationUseCase.generateImage(prompt, model, quality, width, height, n);
    }

    /**
     * Get available image generation models.
     */
    public List<String> getAvailableImageModels() {
        return imageGenerationUseCase.getAvailableModels();
    }

    /**
     * Get available image sizes.
     */
    public List<String> getAvailableImageSizes() {
        return imageGenerationUseCase.getAvailableSizes();
    }

    /**
     * Get available image qualities.
     */
    public List<String> getAvailableImageQualities() {
        return imageGenerationUseCase.getAvailableQualities();
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
