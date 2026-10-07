package com.ai.vision.domain.model;

import lombok.Value;

/** Text recognized in an image. */
@Value
public class OcrResult {
  String text;
}
