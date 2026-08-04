package com.ai.image.domain;

import com.ai.image.domain.GeneratedImage;
import com.ai.image.domain.ImageOptions;
import com.ai.image.domain.ImagePrompt;

public interface ImageGenerationRepository {

    GeneratedImage generate(ImagePrompt prompt, ImageOptions options);
}
