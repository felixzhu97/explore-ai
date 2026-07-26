package com.ai.vision.infrastructure.config;

import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VisionConfig")
class VisionConfigTest {

    @Test
    @DisplayName("should_configure_jna_library_path_without_error")
    void should_configure_jna_library_path_without_error() {
        VisionConfig config = new VisionConfig();
        config.configureNativeLibraries();

        assertThat(System.getProperty("jna.library.path")).isNotNull();
    }

    @Test
    @DisplayName("should_create_tesseract_bean_with_properties")
    void should_create_tesseract_bean_with_properties() throws Exception {
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
