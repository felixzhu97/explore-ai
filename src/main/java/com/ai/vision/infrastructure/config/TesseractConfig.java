package com.ai.vision.infrastructure.config;

import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tesseract OCR bean wiring for the vision module. */
@Configuration
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-vision",
    havingValue = "true",
    matchIfMissing = true)
public class TesseractConfig {

  /** Documentation. */
  @Bean
  ITesseract tesseract(VisionModelProperties properties) {
    Tesseract tesseract = new Tesseract();
    Path tessdataPath = Path.of(properties.getOcr().getTessdataPath());
    if (Files.isDirectory(tessdataPath)) {
      tesseract.setDatapath(tessdataPath.toAbsolutePath().toString());
    }
    tesseract.setLanguage(properties.getOcr().getLanguages());
    tesseract.setPageSegMode(properties.getOcr().getPageSegMode());
    return tesseract;
  }
}
