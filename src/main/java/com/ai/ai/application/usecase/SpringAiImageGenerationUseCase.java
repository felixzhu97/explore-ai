package com.ai.ai.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring AI implementation of image generation use case using DALL-E via ImageModel API.
 */
@Service
public class SpringAiImageGenerationUseCase implements ImageGenerationUseCasePort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiImageGenerationUseCase.class);

    private final ImageModel imageModel;

    public SpringAiImageGenerationUseCase(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    /**
     * Generate an image from a text prompt.
     *
     * @param prompt The text prompt describing the image
     * @return URL of the generated image
     */
    @Override
    public String generateImage(String prompt) {
        log.info("Generating image for prompt: {}", truncate(prompt, 50));

        ImagePrompt imagePrompt = new ImagePrompt(prompt);
        ImageResponse response = imageModel.call(imagePrompt);

        if (response.getResults().isEmpty()) {
            return null;
        }

        String imageUrl = response.getResults().get(0).getOutput().getUrl();
        log.info("Generated image URL: {}", imageUrl);
        return imageUrl;
    }

    /**
     * Generate an image with custom options.
     *
     * @param prompt The text prompt describing the image
     * @param model The model to use (e.g., "dall-e-3", "dall-e-2")
     * @param quality The quality of the image ("standard", "hd")
     * @param width The width of the image
     * @param height The height of the image
     * @param n Number of images to generate
     * @return URL of the generated image
     */
    @Override
    public String generateImage(String prompt, String model, String quality, int width, int height, int n) {
        log.info("Generating image with options - model: {}, quality: {}, size: {}x{}", model, quality, width, height);

        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .model(model != null ? model : "dall-e-3")
                .quality(quality != null ? quality : "standard")
                .width(width)
                .height(height)
                .n(n > 0 ? n : 1)
                .build();

        ImagePrompt imagePrompt = new ImagePrompt(prompt, options);
        ImageResponse response = imageModel.call(imagePrompt);

        if (response.getResults().isEmpty()) {
            return null;
        }

        String imageUrl = response.getResults().get(0).getOutput().getUrl();
        log.info("Generated image URL: {}", imageUrl);
        return imageUrl;
    }

    /**
     * Get available image models.
     */
    @Override
    public List<String> getAvailableModels() {
        return List.of("dall-e-3", "dall-e-2");
    }

    /**
     * Get available image sizes.
     */
    @Override
    public List<String> getAvailableSizes() {
        return List.of("1024x1024", "1024x1792", "1792x1024");
    }

    /**
     * Get available quality options.
     */
    @Override
    public List<String> getAvailableQualities() {
        return List.of("standard", "hd");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
