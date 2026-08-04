package com.ai.vision.domain;

import com.ai.vision.domain.Detection;
import java.awt.image.BufferedImage;
import java.util.List;

public interface ObjectDetector {

    List<Detection> detect(BufferedImage image);

    boolean isAvailable();
}
