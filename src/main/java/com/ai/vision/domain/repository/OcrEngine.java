package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.OcrResult;
import java.awt.image.BufferedImage;

/** Extracts text from images and reports whether the OCR backend is usable. */
public interface OcrEngine {
  OcrResult extract(BufferedImage image);

  boolean isAvailable();
}
