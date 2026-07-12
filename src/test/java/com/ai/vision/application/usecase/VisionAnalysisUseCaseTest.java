package com.ai.vision.application.usecase;

import com.ai.vision.domain.model.CaptionResult;
import com.ai.vision.domain.model.Detection;
import com.ai.vision.domain.model.OcrResult;
import com.ai.vision.domain.port.ImageCaptioner;
import com.ai.vision.domain.port.ObjectDetector;
import com.ai.vision.domain.port.OcrEngine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisionAnalysisUseCase")
class VisionAnalysisUseCaseTest {

    @Mock
    private ImageCaptioner captioner;

    @Mock
    private ObjectDetector detector;

    @Mock
    private OcrEngine ocrEngine;

    private VisionAnalysisUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VisionAnalysisUseCase(
                captioner, detector, ocrEngine, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("should_return_caption_from_captioner_port")
    void should_return_caption_from_captioner_port() throws Exception {
        when(captioner.caption(any(BufferedImage.class)))
                .thenReturn(new CaptionResult("A red bicycle"));

        var response = useCase.caption(pngFile("photo.png"));

        assertThat(response.caption()).isEqualTo("A red bicycle");
        assertThat(response.processingTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("should_map_detections_from_detector_port")
    void should_map_detections_from_detector_port() throws Exception {
        when(detector.detect(any(BufferedImage.class)))
                .thenReturn(List.of(new Detection("cat", 0.91, 1, 2, 3, 4)));

        var response = useCase.detect(pngFile("photo.png"));

        assertThat(response.detections()).hasSize(1);
        assertThat(response.detections().getFirst().className()).isEqualTo("cat");
        assertThat(response.detections().getFirst().bbox()).containsExactly(1.0, 2.0, 3.0, 4.0);
    }

    @Test
    @DisplayName("should_return_ocr_text_from_ocr_engine_port")
    void should_return_ocr_text_from_ocr_engine_port() throws Exception {
        when(ocrEngine.extract(any(BufferedImage.class)))
                .thenReturn(new OcrResult("Hello World"));

        var response = useCase.ocr(pngFile("scan.png"));

        assertThat(response.fullText()).isEqualTo("Hello World");
    }

    private MockMultipartFile pngFile(String name) {
        return new MockMultipartFile("file", name, "image/png", minimalPng());
    }

    private byte[] minimalPng() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };
    }
}
