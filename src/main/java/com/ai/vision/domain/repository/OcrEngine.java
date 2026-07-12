package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.OcrResult;
import java.awt.image.BufferedImage;

public interface OcrEngine {

    OcrResult extract(BufferedImage image);

    boolean isAvailable();
}
