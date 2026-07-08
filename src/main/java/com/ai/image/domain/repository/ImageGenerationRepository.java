package com.ai.image.domain.repository;

import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.vo.ImageOptions;
import com.ai.image.domain.vo.ImagePrompt;

public interface ImageGenerationRepository {

    GeneratedImage generate(ImagePrompt prompt, ImageOptions options);
}
