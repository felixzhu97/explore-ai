package com.ai.image.application.usecase;

import com.ai.image.domain.exception.ImageProviderNotConfiguredException;
import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.repository.ImageGenerationRepository;
import com.ai.image.domain.vo.ImageCatalog;
import com.ai.image.domain.vo.ImageOptions;
import com.ai.image.domain.vo.ImagePrompt;
import com.ai.image.infrastructure.config.ImageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ImageFacade {

    private static final Logger log = LoggerFactory.getLogger(ImageFacade.class);

    private final ImageGenerationRepository imageGenerationRepository;
    private final ImageProperties imageProperties;

    public ImageFacade(
            ImageGenerationRepository imageGenerationRepository, ImageProperties imageProperties) {
        this.imageGenerationRepository = imageGenerationRepository;
        this.imageProperties = imageProperties;
    }

    public GeneratedImage generateImage(
            String prompt, String model, String quality, int width, int height, int n) {
        ensureProviderConfigured();
        log.info("ImageFacade.generateImage: {}", truncate(prompt));
        GeneratedImage image = imageGenerationRepository.generate(
                ImagePrompt.of(prompt),
                ImageOptions.of(resolveModel(model), quality, width, height, n));
        return image.isAvailable() ? image : GeneratedImage.empty();
    }

    private void ensureProviderConfigured() {
        if (!imageProperties.isEnabled()) {
            throw ImageProviderNotConfiguredException.disabled();
        }
        if (!imageProperties.isConfigured()) {
            if (imageProperties.isOpenAiProvider()) {
                throw ImageProviderNotConfiguredException.openAiKeyMissing();
            }
            throw ImageProviderNotConfiguredException.ollamaModelMissing();
        }
        if (imageProperties.isOllamaProvider() && isLocalOllamaEndpoint(imageProperties.getBaseUrl())) {
            throw ImageProviderNotConfiguredException.ollamaModelMissing();
        }
    }

    private boolean isLocalOllamaEndpoint(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return true;
        }
        String normalized = baseUrl.toLowerCase();
        return normalized.contains("localhost") || normalized.contains("127.0.0.1");
    }

    private String resolveModel(String model) {
        if (StringUtils.hasText(model)) {
            return model.trim();
        }
        return imageProperties.getModel();
    }

    public List<String> getAvailableImageModels() {
        return ImageCatalog.defaults().models();
    }

    public List<String> getAvailableImageSizes() {
        return ImageCatalog.defaults().sizes();
    }

    public List<String> getAvailableImageQualities() {
        return ImageCatalog.defaults().qualities();
    }

    private String truncate(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= 50) {
            return text;
        }
        return text.substring(0, 50) + "...";
    }
}
