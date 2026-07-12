package com.ai.vision.web;

import com.ai.vision.application.usecase.VisionAnalysisUseCase;
import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.domain.port.ImageCaptioner;
import com.ai.vision.domain.port.ObjectDetector;
import com.ai.vision.domain.port.OcrEngine;
import com.ai.vision.web.dto.CaptionResponse;
import com.ai.vision.web.dto.DetectResponse;
import com.ai.vision.web.dto.DetectionDto;
import com.ai.vision.web.dto.OcrResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisionController")
class VisionControllerTest {

    @Mock
    private VisionAnalysisUseCase visionAnalysisUseCase;

    @Mock
    private ImageCaptioner captioner;

    @Mock
    private ObjectDetector detector;

    @Mock
    private OcrEngine ocrEngine;

    private VisionController controller;

    @BeforeEach
    void setUp() {
        controller = new VisionController(
                visionAnalysisUseCase, captioner, detector, ocrEngine);
    }

    @Nested
    @DisplayName("POST /api/vision/caption")
    class Caption {

        @Test
        @DisplayName("should return caption response")
        void should_return_caption_response() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", "image".getBytes());
            when(visionAnalysisUseCase.caption(file))
                    .thenReturn(new CaptionResponse("A cat on a sofa", 120L));

            CaptionResponse response = controller.caption(file);

            assertThat(response.caption()).isEqualTo("A cat on a sofa");
            assertThat(response.processingTimeMs()).isEqualTo(120L);
        }

        @Test
        @DisplayName("should throw for empty file")
        void should_throw_for_empty_file() {
            assertThatThrownBy(() -> controller.caption(null))
                    .isInstanceOf(VisionInvalidFileException.class);
        }
    }

    @Nested
    @DisplayName("POST /api/vision/detect")
    class Detect {

        @Test
        @DisplayName("should return detections")
        void should_return_detections() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", "image".getBytes());
            List<DetectionDto> detections = List.of(
                    new DetectionDto("cat", 0.95, List.of(10.0, 20.0, 100.0, 80.0)));
            when(visionAnalysisUseCase.detect(file))
                    .thenReturn(new DetectResponse(detections, 150L));

            DetectResponse response = controller.detect(file);

            assertThat(response.detections()).hasSize(1);
            assertThat(response.processingTimeMs()).isEqualTo(150L);
        }
    }

    @Nested
    @DisplayName("POST /api/vision/ocr")
    class Ocr {

        @Test
        @DisplayName("should return extracted text")
        void should_return_extracted_text() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "scan.png", "image/png", "image".getBytes());
            when(visionAnalysisUseCase.ocr(file))
                    .thenReturn(new OcrResponse("Hello World", 90L));

            OcrResponse response = controller.ocr(file);

            assertThat(response.fullText()).isEqualTo("Hello World");
        }
    }

    @Nested
    @DisplayName("GET /api/vision/health")
    class Health {

        @Test
        @DisplayName("should report provider availability")
        void should_report_provider_availability() {
            when(captioner.isAvailable()).thenReturn(true);
            when(detector.isAvailable()).thenReturn(false);
            when(ocrEngine.isAvailable()).thenReturn(true);

            var response = controller.health();

            assertThat(response.status()).isEqualTo("DEGRADED");
            assertThat(response.providers().get("detect")).isEqualTo("DOWN");
        }
    }
}
