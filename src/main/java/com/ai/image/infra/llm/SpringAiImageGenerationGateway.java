package com.ai.image.infra.llm;

import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.model.ImageOptions;
import com.ai.image.domain.model.ImagePrompt;
import com.ai.image.domain.repository.ImageGenerationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/** Spring AI repository that generates images through an OpenAI-compatible image model. */
@Repository
@RequiredArgsConstructor
public class SpringAiImageGenerationGateway implements ImageGenerationGateway {

  private final ImageModel imageModel;

  @Override
  public GeneratedImage generateImage(ImagePrompt prompt, ImageOptions options) {

    OpenAiImageOptions.Builder optionsBuilder =
        OpenAiImageOptions.builder()
            .model(options.getModel())
            .width(options.getSize().getWidth())
            .height(options.getSize().getHeight())
            .n(options.getCount());

    if (StringUtils.hasText(options.getQuality())) {
      optionsBuilder.quality(options.getQuality());
    }

    var imagePrompt =
        new org.springframework.ai.image.ImagePrompt(prompt.getValue(), optionsBuilder.build());
    ImageResponse response = imageModel.call(imagePrompt);

    if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
      return GeneratedImage.createEmptyImage();
    }

    var firstResult = response.getResults().getFirst();
    if (firstResult == null || firstResult.getOutput() == null) {
      return GeneratedImage.createEmptyImage();
    }

    var output = firstResult.getOutput();
    String imageBase64 = output.getB64Json();
    if (StringUtils.hasText(imageBase64)) {
      return GeneratedImage.createBase64Image(imageBase64, options.getModel(), prompt.getValue());
    }

    String imageUrl = output.getUrl();
    if (StringUtils.hasText(imageUrl)) {
      return GeneratedImage.createUrlImage(imageUrl, options.getModel(), prompt.getValue());
    }

    return GeneratedImage.createEmptyImage();
  }
}
