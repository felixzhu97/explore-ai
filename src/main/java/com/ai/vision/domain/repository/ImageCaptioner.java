package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.CaptionResult;
import java.awt.image.BufferedImage;

/** Generates a natural-language caption for an image and reports model availability. */
public interface ImageCaptioner {
  CaptionResult captionImage(BufferedImage image);

  boolean isAvailable();
}
