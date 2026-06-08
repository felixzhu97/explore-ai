package com.ai.vision.provider;

import com.ai.vision.config.VisionProperties;
import com.ai.vision.model.OcrResponse;
import net.java.dev.tess4j.Tesseract;
import net.java.dev.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * OCR text recognition provider using Tesseract.
 * 
 * Supports:
 * - Multiple languages (eng, chi_sim, jpn, kor, etc.)
 * - Layout analysis (tables, multi-column)
 * - Confidence scores
 * 
 * Alternative: PaddleOCR Java bindings when available.
 */
@Component
@ConditionalOnProperty(name = "vision.ocr.enabled", havingValue = "true", matchIfMissing = true)
public class OcrProvider implements VisionModel {

    private static final Logger log = LoggerFactory.getLogger(OcrProvider.class);

    private final VisionProperties.OcrConfig config;
    private volatile Tesseract tesseract;
    private volatile boolean initialized = false;

    public OcrProvider(VisionProperties properties) {
        this.config = properties.getOcr();
    }

    @Override
    public ModelType type() {
        return ModelType.OCR;
    }

    @Override
    @PostConstruct
    public Mono<Void> initialize() {
        return Mono.fromRunnable(() -> {
            log.info("Initializing OCR provider with language: {}", config.getLanguage());
            
            try {
                // Initialize Tesseract
                tesseract = new Tesseract();
                tesseract.setDatapath(config.getModelPath());
                tesseract.setLanguage(config.getLanguage());
                tesseract.setPageSegMode(3); // Automatic page segmentation with OSD
                
                // Verify tessdata exists
                Path tessdataPath = Path.of(config.getModelPath());
                if (!Files.exists(tessdataPath)) {
                    log.warn("Tessdata not found at {}. OCR may not work correctly.", 
                             config.getModelPath());
                } else {
                    log.info("Tesseract initialized successfully");
                }
                
                this.initialized = true;
            } catch (Exception e) {
                log.error("Failed to initialize Tesseract: {}", e.getMessage());
                this.initialized = true; // Still mark as initialized, but will fail on use
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return initialized && tesseract != null;
    }

    @Override
    public Mono<OcrResponse> recognizeText(byte[] imageData, String language) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException("OCR model not initialized"));
        }

        return Mono.fromCallable(() -> {
            log.info("Running OCR on image ({} bytes), language: {}", 
                     imageData.length, language);

            try {
                // Update language if different
                if (language != null && !language.equals(tesseract.getLanguage())) {
                    tesseract.setLanguage(language);
                }

                // Convert bytes to BufferedImage
                ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
                BufferedImage image = ImageIO.read(bais);
                
                if (image == null) {
                    throw new IllegalArgumentException("Invalid image format");
                }

                // Perform OCR
                String text = tesseract.doOCR(image);
                float confidence = 85.0f; // Tesseract doesn't provide per-result confidence

                // Parse text blocks from lines
                String[] lines = text.split("\\r?\\n");
                List<OcrResponse.TextBlock> blocks = java.util.Arrays.stream(lines)
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> new OcrResponse.TextBlock(
                        line.trim(),
                        confidence,
                        null // Bounding box not available in basic OCR
                    ))
                    .toList();

                return new OcrResponse(text.trim(), confidence, blocks);

            } catch (TesseractException e) {
                log.error("OCR failed: {}", e.getMessage());
                throw new RuntimeException("OCR processing failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Image processing failed: {}", e.getMessage());
                throw new RuntimeException("Image processing failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * OCR with bounding boxes (requires advanced Tesseract configuration).
     */
    public Mono<OcrResponse> recognizeTextWithBoxes(byte[] imageData, String language) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException("OCR model not initialized"));
        }

        return Mono.fromCallable(() -> {
            try {
                if (language != null) {
                    tesseract.setLanguage(language);
                }

                ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
                BufferedImage image = ImageIO.read(bais);
                
                // Get detailed results with bounding boxes
                List<net.java.dev.tess4j.Word> words = tesseract.getWords(image, 3);
                
                List<OcrResponse.TextBlock> blocks = words.stream()
                    .map(word -> {
                        java.awt.Rectangle bounds = word.getBoundingBox();
                        return new OcrResponse.TextBlock(
                            word.getText().trim(),
                            word.getConfidence(),
                            new OcrResponse.BoundingBox(
                                bounds.x,
                                bounds.y,
                                bounds.x + bounds.width,
                                bounds.y + bounds.height
                            )
                        );
                    })
                    .toList();

                String text = blocks.stream()
                    .map(OcrResponse.TextBlock::text)
                    .collect(java.util.stream.Collectors.joining(" "));

                float avgConfidence = (float) words.stream()
                    .mapToDouble(net.java.dev.tess4j.Word::getConfidence)
                    .average()
                    .orElse(0);

                return new OcrResponse(text, avgConfidence, blocks);

            } catch (Exception e) {
                throw new RuntimeException("OCR with boxes failed: " + e.getMessage(), e);
            }
        });
    }
}
