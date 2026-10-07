package com.ai.vision.domain.model;

import lombok.Value;

/** Object found in an image: class, confidence and box from its top-left corner. */
@Value
public class Detection {
  String className;
  double confidence;
  double left;
  double top;
  double width;
  double height;
}
