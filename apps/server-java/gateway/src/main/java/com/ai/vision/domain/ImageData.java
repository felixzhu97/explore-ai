package com.ai.vision.domain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * Value object wrapping immutable image data (byte array).
 * Ensures data integrity and prevents mutation.
 */
public final class ImageData {

    private final byte[] data;
    private final String mimeType;
    private final long size;

    private ImageData(byte[] data, String mimeType) {
        this.data = Objects.requireNonNull(data, "Image data cannot be null").clone();
        this.mimeType = mimeType != null ? mimeType : "image/jpeg";
        this.size = this.data.length;
    }

    public static ImageData of(byte[] data) {
        return new ImageData(data, "image/jpeg");
    }

    public static ImageData of(byte[] data, String mimeType) {
        return new ImageData(data, mimeType);
    }

    public byte[] data() {
        return data.clone();
    }

    public byte[] dataUnsafe() {
        return data;
    }

    public String mimeType() {
        return mimeType;
    }

    public long size() {
        return size;
    }

    public int width() {
        BufferedImage img = getBufferedImage();
        return img != null ? img.getWidth() : 0;
    }

    public int height() {
        BufferedImage img = getBufferedImage();
        return img != null ? img.getHeight() : 0;
    }

    private BufferedImage getBufferedImage() {
        try {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEmpty() {
        return data.length == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageData imageData = (ImageData) o;
        return size == imageData.size && 
               Arrays.equals(data, imageData.data) && 
               Objects.equals(mimeType, imageData.mimeType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mimeType, size);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "ImageData{mimeType='%s', size=%d bytes}".formatted(mimeType, size);
    }
}
