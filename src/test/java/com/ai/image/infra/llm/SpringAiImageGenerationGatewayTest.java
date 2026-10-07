package com.ai.image.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ai.image.domain.model.ImageOptions;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiImageGenerationGateway")
class SpringAiImageGenerationGatewayTest {

  @Mock private ImageModel imageModel;

  private SpringAiImageGenerationGateway repository;

  @BeforeEach
  void setUp() {
    repository = new SpringAiImageGenerationGateway(imageModel);
  }

  @Test
  @DisplayName("should return generated image when model succeeds")
  void shouldReturnGeneratedImageWhenModelSucceeds() {
    String expectedUrl = "https://example.com/image.png";
    Image mockImage = mock(Image.class);
    ImageGeneration mockGeneration = mock(ImageGeneration.class);
    ImageResponse mockResponse = mock(ImageResponse.class);

    when(mockImage.getUrl()).thenReturn(expectedUrl);
    when(mockGeneration.getOutput()).thenReturn(mockImage);
    when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
    when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);

    var result =
        repository.generateImage(
            com.ai.image.domain.model.ImagePrompt.createPrompt("sunset"),
            ImageOptions.createOptions("dall-e-3", "standard", 1024, 1024, 1));

    assertThat(result.getUrl()).isEqualTo(expectedUrl);
  }

  @Test
  @DisplayName("should return base64 image when model returns b64 payload")
  void shouldReturnBase64ImageWhenModelReturnsB64Payload() {
    String expectedBase64 = "abc123";
    Image mockImage = mock(Image.class);
    ImageGeneration mockGeneration = mock(ImageGeneration.class);
    ImageResponse mockResponse = mock(ImageResponse.class);

    when(mockImage.getB64Json()).thenReturn(expectedBase64);
    when(mockGeneration.getOutput()).thenReturn(mockImage);
    when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
    when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);

    var result =
        repository.generateImage(
            com.ai.image.domain.model.ImagePrompt.createPrompt("sunset"),
            ImageOptions.createOptions("dall-e-3", "standard", 1024, 1024, 1));

    assertThat(result.getBase64()).isEqualTo(expectedBase64);
    assertThat(result.hasBase64()).isTrue();
  }

  @Test
  @DisplayName("should return empty image when response has no results")
  void shouldReturnEmptyImageWhenResponseHasNoResults() {
    ImageResponse emptyResponse = mock(ImageResponse.class);
    when(emptyResponse.getResults()).thenReturn(List.of());
    when(imageModel.call(any(ImagePrompt.class))).thenReturn(emptyResponse);

    var result =
        repository.generateImage(
            com.ai.image.domain.model.ImagePrompt.createPrompt("empty"),
            ImageOptions.createOptions(null, null, 1024, 1024, 1));

    assertThat(result.isAvailable()).isFalse();
  }

  @Test
  @DisplayName("should return empty image when response is null")
  void shouldReturnEmptyImageWhenResponseIsNull() {
    when(imageModel.call(any(ImagePrompt.class))).thenReturn(null);

    var result =
        repository.generateImage(
            com.ai.image.domain.model.ImagePrompt.createPrompt("empty"),
            ImageOptions.createOptions(null, null, 1024, 1024, 1));

    assertThat(result.isAvailable()).isFalse();
  }

  @Test
  @DisplayName("should return empty image when first result output is null")
  void shouldReturnEmptyImageWhenFirstResultOutputIsNull() {
    ImageGeneration mockGeneration = mock(ImageGeneration.class);
    ImageResponse mockResponse = mock(ImageResponse.class);

    when(mockGeneration.getOutput()).thenReturn(null);
    when(mockResponse.getResults()).thenReturn(List.of(mockGeneration));
    when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);

    var result =
        repository.generateImage(
            com.ai.image.domain.model.ImagePrompt.createPrompt("empty"),
            ImageOptions.createOptions(null, null, 1024, 1024, 1));

    assertThat(result.isAvailable()).isFalse();
  }
}
