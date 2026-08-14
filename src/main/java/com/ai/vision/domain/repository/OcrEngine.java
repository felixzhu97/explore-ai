package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.OcrResult;
import java.awt.image.BufferedImage;

/** Documentation. */
public interface OcrEngine {
  /** Documentation. */
  OcrResult extract(BufferedImage image);

  /** Documentation. */
  boolean isAvailable();
}
