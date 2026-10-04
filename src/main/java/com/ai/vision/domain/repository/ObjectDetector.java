package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.Detection;
import java.awt.image.BufferedImage;
import java.util.List;

/** Detects labeled objects with bounding boxes in images and reports model availability. */
public interface ObjectDetector {
  List<Detection> detect(BufferedImage image);

  boolean isAvailable();
}
