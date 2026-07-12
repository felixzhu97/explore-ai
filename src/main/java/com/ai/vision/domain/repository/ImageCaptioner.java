package com.ai.vision.domain.repository;

import com.ai.vision.domain.model.CaptionResult;
import java.awt.image.BufferedImage;

public interface ImageCaptioner {

    CaptionResult caption(BufferedImage image);

    boolean isAvailable();
}
