package com.ai.image.domain.repository;

import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.model.ImageOptions;
import com.ai.image.domain.model.ImagePrompt;

/** Repository that generates an image from a prompt using the given options. */
public interface ImageGenerationGateway {
  /** Generates an image from the prompt. */
  GeneratedImage generateImage(ImagePrompt prompt, ImageOptions options);
}
