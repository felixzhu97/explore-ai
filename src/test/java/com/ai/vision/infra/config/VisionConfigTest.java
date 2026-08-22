package com.ai.vision.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VisionConfig")
class VisionConfigTest {

  @Test
  @DisplayName("should configure jna library path without error")
  void shouldConfigureJnaLibraryPathWithoutError() {
    VisionConfig config = new VisionConfig();
    config.configureNativeLibraries();

    assertThat(System.getProperty("jna.library.path")).isNotNull();
  }

  @Test
  @DisplayName("should create tesseract bean with properties")
  void shouldCreateTesseractBeanWithProperties() throws Exception {
    Path tessdata = Files.createTempDirectory("tessdata-test");
    VisionModelProperties properties = new VisionModelProperties();
    properties.getOcr().setTessdataPath(tessdata.toString());
    properties.getOcr().setLanguages("eng");
    properties.getOcr().setPageSegMode(6);

    TesseractConfig config = new TesseractConfig();
    ITesseract tesseract = config.tesseract(properties);

    assertThat(tesseract).isNotNull();
  }
}
