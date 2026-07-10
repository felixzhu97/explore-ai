package com.ai.image.infrastructure.llm;

import com.ai.image.domain.vo.ImageOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiImageGenerationRepository")
class SpringAiImageGenerationRepositoryTest {

    @Mock
    private ImageModel imageModel;

    private SpringAiImageGenerationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SpringAiImageGenerationRepository(imageModel);
    }

    @Test
    @DisplayName("should return generated image when model succeeds")
    void should_return_generated_image_when_model_succeeds() {
        String expectedUrl = "https://example.com/image.png";
        Image mockImage = mock(Image.class);
        ImageGeneration mockGeneration = mock(ImageGeneration.class);
        ImageResponse mockResponse = mock(ImageResponse.class);

        when(mockImage.getUrl()).thenReturn(expectedUrl);
        when(mockGeneration.getOutput()).thenReturn(mockImage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);

        var result = repository.generate(
                com.ai.image.domain.vo.ImagePrompt.of("sunset"),
                ImageOptions.of("dall-e-3", "standard", 1024, 1024, 1));

        assertThat(result.url()).isEqualTo(expectedUrl);
        verify(imageModel).call(any(ImagePrompt.class));
    }

    @Test
    @DisplayName("should return base64 image when model returns b64 payload")
    void should_return_base64_image_when_model_returns_b64_payload() {
        String expectedBase64 = "abc123";
        Image mockImage = mock(Image.class);
        ImageGeneration mockGeneration = mock(ImageGeneration.class);
        ImageResponse mockResponse = mock(ImageResponse.class);

        when(mockImage.getB64Json()).thenReturn(expectedBase64);
        when(mockGeneration.getOutput()).thenReturn(mockImage);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);

        var result = repository.generate(
                com.ai.image.domain.vo.ImagePrompt.of("sunset"),
                ImageOptions.of("dall-e-3", "standard", 1024, 1024, 1));

        assertThat(result.base64()).isEqualTo(expectedBase64);
        assertThat(result.hasBase64()).isTrue();
    }

    @Test
    @DisplayName("should return empty image when response has no results")
    void should_return_empty_image_when_response_has_no_results() {
        ImageResponse emptyResponse = mock(ImageResponse.class);
        when(emptyResponse.getResults()).thenReturn(List.of());
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(emptyResponse);

        var result = repository.generate(
                com.ai.image.domain.vo.ImagePrompt.of("empty"),
                ImageOptions.of(null, null, 1024, 1024, 1));

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("should return empty image when response is null")
    void should_return_empty_image_when_response_is_null() {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(null);

        var result = repository.generate(
                com.ai.image.domain.vo.ImagePrompt.of("empty"),
                ImageOptions.of(null, null, 1024, 1024, 1));

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("should return empty image when first result output is null")
    void should_return_empty_image_when_first_result_output_is_null() {
        ImageGeneration mockGeneration = mock(ImageGeneration.class);
        ImageResponse mockResponse = mock(ImageResponse.class);

        when(mockGeneration.getOutput()).thenReturn(null);
        when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);

        var result = repository.generate(
                com.ai.image.domain.vo.ImagePrompt.of("empty"),
                ImageOptions.of(null, null, 1024, 1024, 1));

        assertThat(result.isAvailable()).isFalse();
    }
}
