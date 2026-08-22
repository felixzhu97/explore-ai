package com.ai.image.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.service.usecase.ImageFacade;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = ImageController.class)
@DisplayName("ImageController")
class ImageControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private ImageFacade imageFacade;

  @Nested
  @DisplayName("POST /api/images/generate")
  class GenerateImage {

    @Test
    @DisplayName("should generate image with all parameters")
    void shouldGenerateImageWithAllParameters() {
      GeneratedImage image =
          GeneratedImage.fromUrl("https://example.com/image.png", "dall-e-3", "A cat");
      when(imageFacade.generateImage("A cat", "dall-e-3", "standard", 1024, 1024, 1))
          .thenReturn(image);

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "prompt": "A cat",
                        "model": "dall-e-3",
                        "quality": "standard",
                        "width": 1024,
                        "height": 1024,
                        "n": 1
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.imageUrl")
          .asString()
          .isEqualTo("https://example.com/image.png");

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "prompt": "A cat",
                        "model": "dall-e-3",
                        "quality": "standard",
                        "width": 1024,
                        "height": 1024,
                        "n": 1
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.model")
          .asString()
          .isEqualTo("dall-e-3");
    }

    @Test
    @DisplayName("should use default values when optional parameters are null")
    void shouldUseDefaultValuesWhenOptionalParametersAreNull() {
      GeneratedImage image =
          GeneratedImage.fromUrl("https://example.com/default.png", "dall-e-3", "Sunset");
      when(imageFacade.generateImage("Sunset", null, null, 1024, 1024, 1)).thenReturn(image);

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"prompt\":\"Sunset\"}"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.imageUrl")
          .asString()
          .isEqualTo("https://example.com/default.png");
      verify(imageFacade).generateImage("Sunset", null, null, 1024, 1024, 1);
    }

    @Test
    @DisplayName("should use image model when request model is null")
    void shouldUseImageModelWhenRequestModelIsNull() {
      GeneratedImage image =
          GeneratedImage.fromUrl("https://example.com/image.png", "dall-e-3", "Mountain");
      when(imageFacade.generateImage("Mountain", null, "hd", 512, 512, 2)).thenReturn(image);

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "prompt": "Mountain",
                        "quality": "hd",
                        "width": 512,
                        "height": 512,
                        "n": 2
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.model")
          .asString()
          .isEqualTo("dall-e-3");
    }

    @Test
    @DisplayName("should return base64 payload when facade returns base64 image")
    void shouldReturnBase64PayloadWhenFacadeReturnsBase64Image() {
      GeneratedImage image = GeneratedImage.fromBase64("abc123", "dall-e-3", "Test");
      when(imageFacade.generateImage("Test", null, null, 1024, 1024, 1)).thenReturn(image);

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"prompt\":\"Test\"}"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.imageBase64")
          .asString()
          .isEqualTo("abc123");
    }

    @Test
    @DisplayName("should return 500 when facade returns empty image")
    void shouldReturn500WhenFacadeReturnsEmptyImage() {
      when(imageFacade.generateImage(any(), any(), any(), anyInt(), anyInt(), anyInt()))
          .thenReturn(GeneratedImage.empty());

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"prompt\":\"Test\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyJson()
          .extractingPath("$.status")
          .asString()
          .contains("Failed to generate image");
    }

    @Test
    @DisplayName("should return 500 when facade throws exception")
    void shouldReturn500WhenFacadeThrowsException() {
      when(imageFacade.generateImage(any(), any(), any(), anyInt(), anyInt(), anyInt()))
          .thenThrow(new RuntimeException("API error"));

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"prompt\":\"Test\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyJson()
          .extractingPath("$.status")
          .asString()
          .contains("ERROR");
    }

    @Test
    @DisplayName("should pass custom dimensions to facade")
    void shouldPassCustomDimensionsToFacade() {
      GeneratedImage image =
          GeneratedImage.fromUrl("https://example.com/wide.png", "dall-e-2", "Landscape");
      when(imageFacade.generateImage("Landscape", "dall-e-2", null, 1920, 1080, 1))
          .thenReturn(image);

      assertThat(
              mvc.post()
                  .uri("/api/images/generate")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "prompt": "Landscape",
                        "model": "dall-e-2",
                        "width": 1920,
                        "height": 1080,
                        "n": 1
                      }
                      """))
          .hasStatusOk();
      verify(imageFacade).generateImage("Landscape", "dall-e-2", null, 1920, 1080, 1);
    }
  }

  @Nested
  @DisplayName("GET /api/images/models")
  class GetImageModels {

    @Test
    @DisplayName("should return available image models")
    void shouldReturnAvailableImageModels() {
      List<String> models = List.of("dall-e-2", "dall-e-3", "dall-e-3-hd");
      when(imageFacade.getAvailableImageModels()).thenReturn(models);

      assertThat(mvc.get().uri("/api/images/models"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.models")
          .asArray()
          .containsExactly("dall-e-2", "dall-e-3", "dall-e-3-hd");
    }

    @Test
    @DisplayName("should return empty list when no models available")
    void shouldReturnEmptyListWhenNoModelsAvailable() {
      when(imageFacade.getAvailableImageModels()).thenReturn(List.of());

      assertThat(mvc.get().uri("/api/images/models"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.models.length()")
          .convertTo(Integer.class)
          .isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("GET /api/images/sizes")
  class GetImageSizes {

    @Test
    @DisplayName("should return available image sizes")
    void shouldReturnAvailableImageSizes() {
      List<String> sizes = List.of("256x256", "512x512", "1024x1024");
      when(imageFacade.getAvailableImageSizes()).thenReturn(sizes);

      assertThat(mvc.get().uri("/api/images/sizes"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.sizes")
          .asArray()
          .containsExactly("256x256", "512x512", "1024x1024");
    }
  }

  @Nested
  @DisplayName("GET /api/images/qualities")
  class GetImageQualities {

    @Test
    @DisplayName("should return available image qualities")
    void shouldReturnAvailableImageQualities() {
      List<String> qualities = List.of("standard", "hd");
      when(imageFacade.getAvailableImageQualities()).thenReturn(qualities);

      assertThat(mvc.get().uri("/api/images/qualities"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.qualities")
          .asArray()
          .containsExactly("standard", "hd");
    }
  }
}
