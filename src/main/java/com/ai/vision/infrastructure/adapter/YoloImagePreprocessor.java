package com.ai.vision.infrastructure.adapter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class YoloImagePreprocessor {

    private YoloImagePreprocessor() {}

    static float[] preprocess(BufferedImage image, int inputSize) {
        BufferedImage resized = resize(image, inputSize, inputSize);
        float[] tensor = new float[3 * inputSize * inputSize];
        int planeSize = inputSize * inputSize;

        for (int y = 0; y < inputSize; y++) {
            for (int x = 0; x < inputSize; x++) {
                int rgb = resized.getRGB(x, y);
                int index = y * inputSize + x;
                tensor[index] = ((rgb >> 16) & 0xFF) / 255f;
                tensor[planeSize + index] = ((rgb >> 8) & 0xFF) / 255f;
                tensor[(2 * planeSize) + index] = (rgb & 0xFF) / 255f;
            }
        }
        return tensor;
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }
}
