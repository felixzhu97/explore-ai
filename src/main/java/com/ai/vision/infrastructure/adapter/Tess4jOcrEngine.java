package com.ai.vision.infrastructure.adapter;

import com.ai.vision.domain.exception.VisionOcrException;
import com.ai.vision.domain.exception.VisionProviderUnavailableException;
import com.ai.vision.domain.model.OcrResult;
import com.ai.vision.domain.port.OcrEngine;
import com.ai.vision.infrastructure.config.VisionModelProperties;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class Tess4jOcrEngine implements OcrEngine {

    private final ITesseract tesseract;
    private final VisionModelProperties properties;
    private final boolean available;

    public Tess4jOcrEngine(ITesseract tesseract, VisionModelProperties properties) {
        this.tesseract = tesseract;
        this.properties = properties;
        this.available = isTessdataAvailable();
    }

    @Override
    public OcrResult extract(BufferedImage image) {
        ensureAvailable();
        try {
            String text = tesseract.doOCR(image);
            return new OcrResult(text == null ? "" : text.trim());
        } catch (TesseractException ex) {
            throw new VisionOcrException("OCR extraction failed", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    private void ensureAvailable() {
        if (!available) {
            throw new VisionProviderUnavailableException(
                    "ocr",
                    "Tesseract OCR is not available. Install tesseract and provide tessdata at "
                            + properties.getOcr().getTessdataPath());
        }
    }

    private boolean isTessdataAvailable() {
        Path tessdataPath = Path.of(properties.getOcr().getTessdataPath());
        if (!Files.isDirectory(tessdataPath)) {
            return false;
        }
        if (!Files.exists(tessdataPath.resolve("eng.traineddata"))) {
            return false;
        }
        try {
            Class.forName("net.sourceforge.tess4j.TessAPI");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
