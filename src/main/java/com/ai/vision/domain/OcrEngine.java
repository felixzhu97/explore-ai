package com.ai.vision.domain;

import com.ai.vision.domain.OcrResult;
import java.awt.image.BufferedImage;

public interface OcrEngine {

    OcrResult extract(BufferedImage image);

    boolean isAvailable();
}
