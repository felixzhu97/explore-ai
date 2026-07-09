package com.ai.image.application.usecase;

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
        log.info("ImageFacade.generateImage: {}", truncate(prompt));
        GeneratedImage image = imageGenerationRepository.generate(
                ImagePrompt.of(prompt),
                ImageOptions.of(resolveModel(model), quality, width, height, n));
        return image.isAvailable() ? image : GeneratedImage.empty();
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
