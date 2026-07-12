package com.ai.vision.infrastructure.adapter;

import com.ai.vision.infrastructure.config.VisionModelProperties;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tess4jOcrEngine")
class Tess4jOcrEngineTest {

    @Mock
    private ITesseract tesseract;

    private Tess4jOcrEngine engine;

    @BeforeEach
    void setUp() {
        VisionModelProperties properties = new VisionModelProperties();
        properties.getOcr().setTessdataPath("models/tessdata");
        engine = new Tess4jOcrEngine(tesseract, properties);
        ReflectionTestUtils.setField(engine, "available", true);
    }

    @Test
    @DisplayName("should_extract_text_from_image")
    void should_extract_text_from_image() throws Exception {
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("Hello");

        var result = engine.extract(helloImage());

        assertThat(result.text()).contains("Hello");
    }

    @Test
    @DisplayName("should_report_available_when_tessdata_exists")
    @EnabledIfEnvironmentVariable(named = "VISION_MODELS_READY", matches = "true")
    void should_report_available_when_tessdata_exists() {
        assertThat(Files.isDirectory(Path.of("models/tessdata"))).isTrue();
    }

    private BufferedImage helloImage() {
        BufferedImage image = new BufferedImage(200, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 200, 80);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        graphics.drawString("Hello", 20, 50);
        graphics.dispose();
        return image;
    }
}
