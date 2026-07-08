package com.ai.image.infrastructure.llm;

import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.repository.ImageGenerationRepository;
import com.ai.image.domain.vo.ImageOptions;
import com.ai.image.domain.vo.ImagePrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Repository;

@Repository
public class SpringAiImageGenerationRepository implements ImageGenerationRepository {

    private static final Logger log = LoggerFactory.getLogger(SpringAiImageGenerationRepository.class);

    private final ImageModel imageModel;

    public SpringAiImageGenerationRepository(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @Override
    public GeneratedImage generate(ImagePrompt prompt, ImageOptions options) {
        log.info(
                "Generating image - model: {}, quality: {}, size: {}x{}",
                options.model(),
                options.quality(),
                options.size().width(),
                options.size().height());

        OpenAiImageOptions springOptions = OpenAiImageOptions.builder()
                .model(options.model())
                .quality(options.quality())
                .width(options.size().width())
                .height(options.size().height())
                .n(options.count())
                .build();

        var imagePrompt = new org.springframework.ai.image.ImagePrompt(prompt.value(), springOptions);
        ImageResponse response = imageModel.call(imagePrompt);

        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return GeneratedImage.empty();
        }

        var firstResult = response.getResults().getFirst();
        if (firstResult == null || firstResult.getOutput() == null) {
            return GeneratedImage.empty();
        }

        String imageUrl = firstResult.getOutput().getUrl();
        log.info("Generated image URL: {}", imageUrl);
        return GeneratedImage.create(imageUrl, options.model(), prompt.value());
    }
}
