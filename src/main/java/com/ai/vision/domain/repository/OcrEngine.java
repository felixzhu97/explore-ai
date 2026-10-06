package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.OcrResult;
import java.awt.image.BufferedImage;

/** Extracts text from images and reports whether the OCR backend is usable. */
public interface OcrEngine {
  /** Reads the text in the image. */
  OcrResult extract(BufferedImage image);

  /** Tells whether the OCR engine can run. */
  boolean isAvailable();
}
