package com.ai.image.domain.model;

import com.ai.common.exception.DomainException;
import lombok.Value;

/** Width and height of a generated image, in pixels. */
@Value
public class ImageSize {
  int width;
  int height;

  /** Creates a size, rejecting non-positive or catalog-unsupported dimensions. */
  public static ImageSize createSize(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw DomainException.createInvalidError(
          "INVALID_IMAGE_PROMPT", "Image dimensions must be positive");
    }
    ImageSize size = new ImageSize(width, height);
    if (!size.isSupported()) {
      throw DomainException.createInvalidError(
          "INVALID_IMAGE_PROMPT", "Unsupported image size: " + width + "x" + height);
    }
    return size;
  }

  /** Tells whether the size is supported. */
  public boolean isSupported() {
    return ImageCatalog.createDefaultCatalog().supportsSize(width, height);
  }
}
