package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.CaptionResult;
import java.awt.image.BufferedImage;

/** Generates a natural-language caption for an image and reports model availability. */
public interface ImageCaptioner {
  /** Describes the image. */
  CaptionResult captionImage(BufferedImage image);

  /** Tells whether the captioner can run. */
  boolean isAvailable();
}
