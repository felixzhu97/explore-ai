package com.ai.image.domain.repository;

import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.vo.ImageOptions;
import com.ai.image.domain.vo.ImagePrompt;

/** Documentation. */
public interface ImageGenerationRepository {
  /** Documentation. */
  GeneratedImage generate(ImagePrompt prompt, ImageOptions options);
}
