package com.ai.image.web;

import com.ai.image.application.usecase.ImageFacade;
import com.ai.image.web.dto.ImageGenerationRequest;
import com.ai.image.web.dto.ImageGenerationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageController")
class ImageControllerTest {

    @Mock
    private ImageFacade imageFacade;

    private ImageController controller;

    @BeforeEach
    void setUp() {
        controller = new ImageController(imageFacade);
    }

    @Nested
    @DisplayName("POST /api/images/generate")
    class GenerateImage {

        @Test
        @DisplayName("should generate image with all parameters")
        void shouldGenerateImageWithAllParameters() {
            String imageUrl = "https://example.com/image.png";
            when(imageFacade.generateImage("A cat", "dall-e-3", "standard", 1024, 1024, 1))
                    .thenReturn(imageUrl);

            ImageGenerationRequest request = new ImageGenerationRequest(
                    "A cat", "dall-e-3", "standard", 1024, 1024, 1);
            ResponseEntity<com.ai.image.web.dto.ImageGenerationResponse> response = 
                    controller.generateImage(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().imageUrl()).isEqualTo(imageUrl);
            assertThat(response.getBody().model()).isEqualTo("dall-e-3");
        }

        @Test
        @DisplayName("should use default values when optional parameters are null")
        void shouldUseDefaultValuesWhenOptionalParametersAreNull() {
            String imageUrl = "https://example.com/default.png";
            when(imageFacade.generateImage("Sunset", null, null, 1024, 1024, 1))
                    .thenReturn(imageUrl);

            ImageGenerationRequest request = new ImageGenerationRequest("Sunset", null, null, null, null, null);
            ResponseEntity<com.ai.image.web.dto.ImageGenerationResponse> response = 
                    controller.generateImage(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().imageUrl()).isEqualTo(imageUrl);
            verify(imageFacade).generateImage("Sunset", null, null, 1024, 1024, 1);
        }

        @Test
        @DisplayName("should use default model when model is null")
        void shouldUseDefaultModelWhenModelIsNull() {
            String imageUrl = "https://example.com/image.png";
            when(imageFacade.generateImage("Mountain", null, "hd", 512, 512, 2))
                    .thenReturn(imageUrl);

            ImageGenerationRequest request = new ImageGenerationRequest("Mountain", null, "hd", 512, 512, 2);
            ResponseEntity<com.ai.image.web.dto.ImageGenerationResponse> response = 
                    controller.generateImage(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().model()).isEqualTo("dall-e-3");
        }

        @Test
        @DisplayName("should return 500 when facade returns null")
        void shouldReturn500WhenFacadeReturnsNull() {
            when(imageFacade.generateImage(any(), any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(null);

            ImageGenerationRequest request = new ImageGenerationRequest("Test", null, null, null, null, null);
            ResponseEntity<ImageGenerationResponse> response = 
                    controller.generateImage(request);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody().status()).contains("Failed to generate image");
        }

        @Test
        @DisplayName("should return 500 when facade throws exception")
        void shouldReturn500WhenFacadeThrowsException() {
            when(imageFacade.generateImage(any(), any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("API error"));

            ImageGenerationRequest request = new ImageGenerationRequest("Test", null, null, null, null, null);
            ResponseEntity<ImageGenerationResponse> response = 
                    controller.generateImage(request);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody().status()).contains("ERROR");
        }

        @Test
        @DisplayName("should pass custom dimensions to facade")
        void shouldPassCustomDimensionsToFacade() {
            String imageUrl = "https://example.com/wide.png";
            when(imageFacade.generateImage("Landscape", "dall-e-2", null, 1920, 1080, 1))
                    .thenReturn(imageUrl);

            ImageGenerationRequest request = new ImageGenerationRequest("Landscape", "dall-e-2", null, 1920, 1080, 1);
            ResponseEntity<com.ai.image.web.dto.ImageGenerationResponse> response = 
                    controller.generateImage(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
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

            ResponseEntity<Map<String, List<String>>> response = controller.getImageModels();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("models", models);
        }

        @Test
        @DisplayName("should return empty list when no models available")
        void shouldReturnEmptyListWhenNoModelsAvailable() {
            when(imageFacade.getAvailableImageModels()).thenReturn(List.of());

            ResponseEntity<Map<String, List<String>>> response = controller.getImageModels();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("models", List.of());
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

            ResponseEntity<Map<String, List<String>>> response = controller.getImageSizes();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("sizes", sizes);
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

            ResponseEntity<Map<String, List<String>>> response = controller.getImageQualities();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("qualities", qualities);
        }
    }
}
