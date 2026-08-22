package com.ai.image.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImageModel;

@DisplayName("ImageModelConfig")
class ImageModelConfigTest {

  @Test
  @DisplayName("should create image model from properties")
  void shouldCreateImageModelFromProperties() {
    ImageModelConfig config = new ImageModelConfig();
    ImageProperties properties = new ImageProperties();
    properties.setApiKey("test-key");
    properties.setBaseUrl("http://localhost:11434/v1/");
    properties.setModel("x/flux2-klein");

    ImageModel model = config.imageModel(properties);

    assertThat(model).isNotNull();
  }

  @Test
  @DisplayName("should use default base url when blank")
  void shouldUseDefaultBaseUrlWhenBlank() {
    ImageModelConfig config = new ImageModelConfig();
    ImageProperties properties = new ImageProperties();
    properties.setApiKey("test-key");
    properties.setBaseUrl("");

    ImageModel model = config.imageModel(properties);

    assertThat(model).isNotNull();
  }
}
