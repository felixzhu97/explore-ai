package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.CaptionResult;
import java.awt.image.BufferedImage;

/** Documentation. */
public interface ImageCaptioner {
  /** Documentation. */
  CaptionResult caption(BufferedImage image);

  /** Documentation. */
  boolean isAvailable();
}
