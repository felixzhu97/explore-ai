package com.ai.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.domain.exception.VisionProviderUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("GlobalExceptionHandler Vision")
class GlobalExceptionHandlerVisionTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("should map vision provider unavailable to 503")
  void shouldMapVisionProviderUnavailableTo503() {
    var response =
        handler.handleVisionProviderUnavailable(
            new VisionProviderUnavailableException("ocr", "OCR unavailable"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().errorCode()).isEqualTo("VISION_PROVIDER_UNAVAILABLE");
  }

  @Test
  @DisplayName("should map invalid file to 400")
  void shouldMapInvalidFileTo400() {
    var response =
        handler.handleVisionInvalidFile(new VisionInvalidFileException("Image file is required"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().errorCode()).isEqualTo("INVALID_FILE");
  }
}
