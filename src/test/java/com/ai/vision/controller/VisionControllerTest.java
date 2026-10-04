package com.ai.vision.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.ai.common.controller.GlobalExceptionHandler;
import com.ai.common.controller.dto.HealthStatus;
import com.ai.testsupport.SliceWebMvcTest;
import com.ai.vision.controller.dto.CaptionResponse;
import com.ai.vision.controller.dto.DetectResponse;
import com.ai.vision.controller.dto.DetectionResponse;
import com.ai.vision.controller.dto.OcrResponse;
import com.ai.vision.controller.dto.VisionHealthResponse;
import com.ai.vision.controller.dto.VisionProvidersResponse;
import com.ai.vision.service.VisionAnalysisService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = VisionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("VisionController")
class VisionControllerTest {

  @DynamicPropertySource
  static void enableVisionModule(DynamicPropertyRegistry registry) {
    registry.add("launchdarkly.bootstrap.module-vision", () -> "true");
  }

  @Autowired private MockMvcTester mvc;

  @MockitoBean private VisionAnalysisService visionAnalysisService;

  @Nested
  @DisplayName("POST /api/vision/caption")
  class Caption {

    @Test
    @DisplayName("should return caption response")
    void shouldReturnCaptionResponse() throws Exception {
      MockMultipartFile file =
          new MockMultipartFile("file", "photo.jpg", "image/jpeg", "image".getBytes());
      doReturn(new CaptionResponse("A cat on a sofa", 120L))
          .when(visionAnalysisService)
          .caption(any());

      assertThat(mvc.post().multipart().uri("/api/vision/caption").file(file))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.caption")
          .asString()
          .isEqualTo("A cat on a sofa");

      assertThat(mvc.post().multipart().uri("/api/vision/caption").file(file))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.processingTimeMs")
          .convertTo(Long.class)
          .isEqualTo(120L);
    }

    @Test
    @DisplayName("should return 400 for empty file")
    void shouldReturn400ForEmptyFile() {
      assertThat(mvc.post().multipart().uri("/api/vision/caption"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.errorCode")
          .asString()
          .isEqualTo("INVALID_FILE");
    }
  }

  @Nested
  @DisplayName("POST /api/vision/detect")
  class Detect {

    @Test
    @DisplayName("should return detections")
    void shouldReturnDetections() throws Exception {
      MockMultipartFile file =
          new MockMultipartFile("file", "photo.jpg", "image/jpeg", "image".getBytes());
      List<DetectionResponse> detections =
          List.of(new DetectionResponse("cat", 0.95, List.of(10.0, 20.0, 100.0, 80.0)));
      doReturn(new DetectResponse(detections, 150L)).when(visionAnalysisService).detect(any());

      assertThat(mvc.post().multipart().uri("/api/vision/detect").file(file))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.detections")
          .asArray()
          .hasSize(1);

      assertThat(mvc.post().multipart().uri("/api/vision/detect").file(file))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.processingTimeMs")
          .convertTo(Long.class)
          .isEqualTo(150L);
    }
  }

  @Nested
  @DisplayName("POST /api/vision/ocr")
  class Ocr {

    @Test
    @DisplayName("should return extracted text")
    void shouldReturnExtractedText() throws Exception {
      MockMultipartFile file =
          new MockMultipartFile("file", "scan.png", "image/png", "image".getBytes());
      doReturn(new OcrResponse("Hello World", 90L)).when(visionAnalysisService).ocr(any());

      assertThat(mvc.post().multipart().uri("/api/vision/ocr").file(file))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.fullText")
          .asString()
          .isEqualTo("Hello World");
    }
  }

  @Nested
  @DisplayName("GET /api/vision/health")
  class Health {

    @Test
    @DisplayName("should report provider availability")
    void shouldReportProviderAvailability() {
      when(visionAnalysisService.health())
          .thenReturn(
              new VisionHealthResponse(
                  HealthStatus.DEGRADED,
                  new VisionProvidersResponse(
                      HealthStatus.UP, HealthStatus.DOWN, HealthStatus.UP)));

      assertThat(mvc.get().uri("/api/vision/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.status")
          .asString()
          .isEqualTo("DEGRADED");

      assertThat(mvc.get().uri("/api/vision/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.providers.detect")
          .asString()
          .isEqualTo("DOWN");
    }
  }
}
