package com.ai.vision.domain;

import com.ai.vision.domain.CaptionResult;
import java.awt.image.BufferedImage;

public interface ImageCaptioner {

    CaptionResult caption(BufferedImage image);

    boolean isAvailable();
}
