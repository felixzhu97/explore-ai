package com.ai.ai.application.usecase;

import com.ai.ai.application.usecase.SpringAiImageGenerationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageGenerationUseCase Tests")
class ImageGenerationUseCaseTest {

    @Mock
    private ImageModel imageModel;

    private SpringAiImageGenerationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SpringAiImageGenerationUseCase(imageModel);
    }

    @Nested
    @DisplayName("generateImage(prompt)")
    class GenerateImageWithPromptTests {

        @Test
        @DisplayName("should return image URL when generation succeeds")
        void shouldReturnImageUrl_whenGenerationSucceeds() {
            // Given
            String prompt = "A beautiful sunset over mountains";
            String expectedUrl = "https://example.com/generated-image.png";

            Image mockImage = Mockito.mock(Image.class);
            ImageGeneration mockGeneration = Mockito.mock(ImageGeneration.class);
            ImageResponse mockResponse = Mockito.mock(ImageResponse.class);

            doReturn(expectedUrl).when(mockImage).getUrl();
            doReturn(mockImage).when(mockGeneration).getOutput();
            doReturn(List.of(mockGeneration)).when(mockResponse).getResults();
            doReturn(mockResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt);

            // Then
            assertThat(result).isEqualTo(expectedUrl);
            verify(imageModel).call(any(ImagePrompt.class));
        }

        @Test
        @DisplayName("should return null when response results are empty")
        void shouldReturnNull_whenResponseResultsAreEmpty() {
            // Given
            String prompt = "An empty prompt";
            ImageResponse emptyResponse = Mockito.mock(ImageResponse.class);
            doReturn(List.of()).when(emptyResponse).getResults();
            doReturn(emptyResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when imageModel call fails")
        void shouldThrowException_whenImageModelCallFails() {
            // Given
            String prompt = "A complex image";
            doThrow(new RuntimeException("Image generation failed")).when(imageModel).call(any(ImagePrompt.class));

            // When/Then
            assertThatThrownBy(() -> useCase.generateImage(prompt))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Image generation failed");
        }

        @Test
        @DisplayName("should return null when image URL is null")
        void shouldReturnNull_whenImageUrlIsNull() {
            // Given
            String prompt = "A test image";
            Image mockImage = Mockito.mock(Image.class);
            ImageGeneration mockGeneration = Mockito.mock(ImageGeneration.class);
            ImageResponse mockResponse = Mockito.mock(ImageResponse.class);

            doReturn(null).when(mockImage).getUrl();
            doReturn(mockImage).when(mockGeneration).getOutput();
            doReturn(List.of(mockGeneration)).when(mockResponse).getResults();
            doReturn(mockResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle empty prompt gracefully")
        void shouldHandleEmptyPrompt() {
            // Given
            String prompt = "";
            String expectedUrl = "https://example.com/empty-prompt.png";

            Image mockImage = Mockito.mock(Image.class);
            ImageGeneration mockGeneration = Mockito.mock(ImageGeneration.class);
            ImageResponse mockResponse = Mockito.mock(ImageResponse.class);

            doReturn(expectedUrl).when(mockImage).getUrl();
            doReturn(mockImage).when(mockGeneration).getOutput();
            doReturn(List.of(mockGeneration)).when(mockResponse).getResults();
            doReturn(mockResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt);

            // Then
            assertThat(result).isEqualTo(expectedUrl);
        }
    }

    @Nested
    @DisplayName("generateImage(prompt, model, quality, width, height, n)")
    class GenerateImageWithOptionsTests {

        @Test
        @DisplayName("should return image URL with custom options")
        void shouldReturnImageUrl_withCustomOptions() {
            // Given
            String prompt = "A futuristic city";
            String model = "dall-e-3";
            String quality = "hd";
            int width = 1024;
            int height = 1792;
            int n = 1;
            String expectedUrl = "https://example.com/futuristic-city.png";

            Image mockImage = Mockito.mock(Image.class);
            ImageGeneration mockGeneration = Mockito.mock(ImageGeneration.class);
            ImageResponse mockResponse = Mockito.mock(ImageResponse.class);

            doReturn(expectedUrl).when(mockImage).getUrl();
            doReturn(mockImage).when(mockGeneration).getOutput();
            doReturn(List.of(mockGeneration)).when(mockResponse).getResults();
            doReturn(mockResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt, model, quality, width, height, n);

            // Then
            assertThat(result).isEqualTo(expectedUrl);
            verify(imageModel).call(any(ImagePrompt.class));
        }

        @Test
        @DisplayName("should use default model when model is null")
        void shouldUseDefaultModel_whenModelIsNull() {
            // Given
            String prompt = "A landscape";
            String expectedUrl = "https://example.com/landscape.png";

            Image mockImage = Mockito.mock(Image.class);
            ImageGeneration mockGeneration = Mockito.mock(ImageGeneration.class);
            ImageResponse mockResponse = Mockito.mock(ImageResponse.class);

            doReturn(expectedUrl).when(mockImage).getUrl();
            doReturn(mockImage).when(mockGeneration).getOutput();
            doReturn(List.of(mockGeneration)).when(mockResponse).getResults();
            doReturn(mockResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt, null, "standard", 1024, 1024, 1);

            // Then
            assertThat(result).isEqualTo(expectedUrl);
        }

        @Test
        @DisplayName("should use default quality when quality is null")
        void shouldUseDefaultQuality_whenQualityIsNull() {
            // Given
            String prompt = "An abstract painting";
            String expectedUrl = "https://example.com/abstract.png";

            Image mockImage = Mockito.mock(Image.class);
            ImageGeneration mockGeneration = Mockito.mock(ImageGeneration.class);
            ImageResponse mockResponse = Mockito.mock(ImageResponse.class);

            doReturn(expectedUrl).when(mockImage).getUrl();
            doReturn(mockImage).when(mockGeneration).getOutput();
            doReturn(List.of(mockGeneration)).when(mockResponse).getResults();
            doReturn(mockResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt, "dall-e-3", null, 1024, 1024, 1);

            // Then
            assertThat(result).isEqualTo(expectedUrl);
        }

        @Test
        @DisplayName("should use default n when n is zero")
        void shouldUseDefaultN_whenNIsZero() {
            // Given
            String prompt = "A portrait";
            String expectedUrl = "https://example.com/portrait.png";

            Image mockImage = Mockito.mock(Image.class);
            ImageGeneration mockGeneration = Mockito.mock(ImageGeneration.class);
            ImageResponse mockResponse = Mockito.mock(ImageResponse.class);

            doReturn(expectedUrl).when(mockImage).getUrl();
            doReturn(mockImage).when(mockGeneration).getOutput();
            doReturn(List.of(mockGeneration)).when(mockResponse).getResults();
            doReturn(mockResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt, "dall-e-3", "standard", 1024, 1024, 0);

            // Then
            assertThat(result).isEqualTo(expectedUrl);
        }

        @Test
        @DisplayName("should return null when options response results are empty")
        void shouldReturnNull_whenOptionsResponseResultsAreEmpty() {
            // Given
            String prompt = "An experimental image";
            ImageResponse emptyResponse = Mockito.mock(ImageResponse.class);
            doReturn(List.of()).when(emptyResponse).getResults();
            doReturn(emptyResponse).when(imageModel).call(any(ImagePrompt.class));

            // When
            String result = useCase.generateImage(prompt, "dall-e-2", "hd", 1792, 1024, 2);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getAvailableModels")
    class GetAvailableModelsTests {

        @Test
        @DisplayName("should return available models")
        void shouldReturnAvailableModels() {
            List<String> models = useCase.getAvailableModels();

            assertThat(models).containsExactly("dall-e-3", "dall-e-2");
        }
    }

    @Nested
    @DisplayName("getAvailableSizes")
    class GetAvailableSizesTests {

        @Test
        @DisplayName("should return available sizes")
        void shouldReturnAvailableSizes() {
            List<String> sizes = useCase.getAvailableSizes();

            assertThat(sizes).containsExactly("1024x1024", "1024x1792", "1792x1024");
        }
    }

    @Nested
    @DisplayName("getAvailableQualities")
    class GetAvailableQualitiesTests {

        @Test
        @DisplayName("should return available qualities")
        void shouldReturnAvailableQualities() {
            List<String> qualities = useCase.getAvailableQualities();

            assertThat(qualities).containsExactly("standard", "hd");
        }
    }
}
