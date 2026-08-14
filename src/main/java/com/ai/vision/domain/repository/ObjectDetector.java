package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.Detection;
import java.awt.image.BufferedImage;
import java.util.List;

/** Documentation. */
public interface ObjectDetector {
  /** Documentation. */
  List<Detection> detect(BufferedImage image);

  /** Documentation. */
  boolean isAvailable();
}
