package com.ai.ai.web;

import com.ai.ai.application.usecase.ImageGenerationUseCasePort;
import com.ai.ai.web.ImageController;
import com.ai.ai.web.dto.ImageGenerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageController")
class ImageControllerTest {

    @Mock
    private ImageGenerationUseCasePort imageGenerationUseCase;

    private ImageController controller;

    @BeforeEach
    void setUp() {
        controller = new ImageController(imageGenerationUseCase);
    }

    @Nested
    @DisplayName("POST /api/images/generate")
    class GenerateImage {

        @Test
        @DisplayName("should return generated image URL when successful")
        void shouldReturnGeneratedImageUrlWhenSuccessful() {
            when(imageGenerationUseCase.generateImage(anyString(), any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenReturn("https://example.com/image.png");

            var response = controller.generateImage(
                    new ImageGenerationRequest("a beautiful sunset", "dall-e-3", "standard", 1024, 1024, 1));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().imageUrl()).isEqualTo("https://example.com/image.png");
            assertThat(response.getBody().model()).isEqualTo("dall-e-3");
            assertThat(response.getBody().status()).isEqualTo("SUCCESS");
            verify(imageGenerationUseCase).generateImage(eq("a beautiful sunset"), eq("dall-e-3"), eq("standard"), eq(1024), eq(1024), eq(1));
        }

        @Test
        @DisplayName("should return default model when model is null")
        void shouldReturnDefaultModelWhenModelIsNull() {
            when(imageGenerationUseCase.generateImage(anyString(), isNull(), any(), anyInt(), anyInt(), anyInt()))
                    .thenReturn("https://example.com/image.png");

            var response = controller.generateImage(
                    new ImageGenerationRequest("a cat", null, null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().model()).isEqualTo("dall-e-3");
        }

        @Test
        @DisplayName("should return 500 error when image URL is null")
        void shouldReturn500ErrorWhenImageUrlIsNull() {
            when(imageGenerationUseCase.generateImage(anyString(), any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(null);

            var response = controller.generateImage(
                    new ImageGenerationRequest("a sunset", "dall-e-3", null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).startsWith("ERROR:");
            assertThat(response.getBody().imageUrl()).isNull();
        }

        @Test
        @DisplayName("should return 500 error when exception is thrown")
        void shouldReturn500ErrorWhenExceptionIsThrown() {
            when(imageGenerationUseCase.generateImage(anyString(), any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("API error"));

            var response = controller.generateImage(
                    new ImageGenerationRequest("test prompt", "dall-e-3", null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).contains("API error");
        }
    }

    @Nested
    @DisplayName("GET /api/images/models")
    class GetModels {

        @Test
        @DisplayName("should return available models")
        void shouldReturnAvailableModels() {
            when(imageGenerationUseCase.getAvailableModels())
                    .thenReturn(List.of("dall-e-2", "dall-e-3"));

            var response = controller.getModels();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("models"))
                    .containsExactly("dall-e-2", "dall-e-3");
            verify(imageGenerationUseCase).getAvailableModels();
        }

        @Test
        @DisplayName("should return empty array when no models available")
        void shouldReturnEmptyArrayWhenNoModelsAvailable() {
            when(imageGenerationUseCase.getAvailableModels())
                    .thenReturn(List.of());

            var response = controller.getModels();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("models")).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/images/sizes")
    class GetSizes {

        @Test
        @DisplayName("should return available sizes")
        void shouldReturnAvailableSizes() {
            when(imageGenerationUseCase.getAvailableSizes())
                    .thenReturn(List.of("256x256", "512x512", "1024x1024"));

            var response = controller.getSizes();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("sizes"))
                    .containsExactly("256x256", "512x512", "1024x1024");
            verify(imageGenerationUseCase).getAvailableSizes();
        }
    }

    @Nested
    @DisplayName("GET /api/images/qualities")
    class GetQualities {

        @Test
        @DisplayName("should return available quality options")
        void shouldReturnAvailableQualityOptions() {
            when(imageGenerationUseCase.getAvailableQualities())
                    .thenReturn(List.of("standard", "hd"));

            var response = controller.getQualities();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("qualities"))
                    .containsExactly("standard", "hd");
            verify(imageGenerationUseCase).getAvailableQualities();
        }
    }
}
