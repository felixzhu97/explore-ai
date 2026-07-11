package com.ai.vision.infrastructure.adapter;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class BlipImagePreprocessor {

    private static final float[] MEAN = {0.48145466f, 0.4578275f, 0.40821073f};
    private static final float[] STD = {0.26862954f, 0.26130258f, 0.27577711f};
    private static final int IMAGE_SIZE = 384;

    private BlipImagePreprocessor() {}

    static float[] preprocess(BufferedImage image) {
        BufferedImage resized = resize(image, IMAGE_SIZE, IMAGE_SIZE);
        float[] tensor = new float[3 * IMAGE_SIZE * IMAGE_SIZE];
        int planeSize = IMAGE_SIZE * IMAGE_SIZE;

        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                float red = ((rgb >> 16) & 0xFF) / 255f;
                float green = ((rgb >> 8) & 0xFF) / 255f;
                float blue = (rgb & 0xFF) / 255f;
                int index = y * IMAGE_SIZE + x;
                tensor[index] = (red - MEAN[0]) / STD[0];
                tensor[planeSize + index] = (green - MEAN[1]) / STD[1];
                tensor[(2 * planeSize) + index] = (blue - MEAN[2]) / STD[2];
            }
        }
        return tensor;
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        Image scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return resized;
    }
}
