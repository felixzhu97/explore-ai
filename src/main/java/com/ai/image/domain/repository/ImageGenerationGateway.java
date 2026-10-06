package com.ai.image.domain.repository;

import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.vo.ImageOptions;
import com.ai.image.domain.vo.ImagePrompt;

/** Repository that generates an image from a prompt using the given options. */
public interface ImageGenerationGateway {
  /** Generates an image from the prompt. */
  GeneratedImage generate(ImagePrompt prompt, ImageOptions options);
}
