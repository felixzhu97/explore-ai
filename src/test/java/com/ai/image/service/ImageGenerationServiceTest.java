package com.ai.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.image.domain.exception.ImageProviderNotConfiguredException;
import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.repository.ImageGenerationGateway;
import com.ai.image.domain.vo.ImageOptions;
import com.ai.image.domain.vo.ImagePrompt;
import com.ai.image.infra.config.ImageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageGenerationService")
class ImageGenerationServiceTest {

  @Mock private ImageGenerationGateway imageGenerationGateway;

  private ImageProperties imageProperties;
  private ImageGenerationService imageGenerationService;

  @BeforeEach
  void setUp() {
    imageProperties = new ImageProperties();
    imageProperties.setProvider(ImageProperties.PROVIDER_OPENAI);
    imageProperties.setApiKey("test-api-key");
    imageProperties.setBaseUrl("https://api.openai.com/v1");
    imageProperties.setModel("x/z-image-turbo");
    imageGenerationService = new ImageGenerationService(imageGenerationGateway, imageProperties);
  }

  @Test
  @DisplayName("should reject local Ollama endpoint in cloud configuration")
  void shouldRejectLocalOllamaEndpointInCloudConfiguration() {
    imageProperties.setProvider(ImageProperties.PROVIDER_OLLAMA);
    imageProperties.setBaseUrl("http://localhost:11434/v1");

    assertThatThrownBy(
            () -> imageGenerationService.generateImage("sunset", null, null, 512, 512, 1))
        .isInstanceOf(ImageProviderNotConfiguredException.class);
  }

  @Test
  @DisplayName("should reject OpenAI provider when API key is missing")
  void shouldRejectOpenaiProviderWhenApiKeyIsMissing() {
    imageProperties.setProvider(ImageProperties.PROVIDER_OPENAI);
    imageProperties.setApiKey("");

    assertThatThrownBy(
            () -> imageGenerationService.generateImage("sunset", null, null, 512, 512, 1))
        .isInstanceOf(ImageProviderNotConfiguredException.class);
  }

  @Test
  @DisplayName("should use configured model when request model is null")
  void shouldUseConfiguredModelWhenRequestModelIsNull() {
    when(imageGenerationGateway.generate(any(ImagePrompt.class), any(ImageOptions.class)))
        .thenReturn(GeneratedImage.fromBase64("abc", "x/z-image-turbo", "sunset"));

    imageGenerationService.generateImage("sunset", null, null, 512, 512, 1);

    ArgumentCaptor<ImageOptions> optionsCaptor = ArgumentCaptor.forClass(ImageOptions.class);
    verify(imageGenerationGateway).generate(any(ImagePrompt.class), optionsCaptor.capture());
    assertThat(optionsCaptor.getValue().model()).isEqualTo("x/z-image-turbo");
  }

  @Test
  @DisplayName("should use request model when provided")
  void shouldUseRequestModelWhenProvided() {
    when(imageGenerationGateway.generate(any(ImagePrompt.class), any(ImageOptions.class)))
        .thenReturn(GeneratedImage.fromUrl("https://example.com/a.png", "dall-e-3", "sunset"));

    imageGenerationService.generateImage("sunset", "dall-e-3", "standard", 1024, 1024, 1);

    ArgumentCaptor<ImageOptions> optionsCaptor = ArgumentCaptor.forClass(ImageOptions.class);
    verify(imageGenerationGateway).generate(any(ImagePrompt.class), optionsCaptor.capture());
    assertThat(optionsCaptor.getValue().model()).isEqualTo("dall-e-3");
  }
}
