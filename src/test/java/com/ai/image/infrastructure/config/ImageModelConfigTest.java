package com.ai.image.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImageModel;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImageModelConfig")
class ImageModelConfigTest {

    @Test
    @DisplayName("should_create_image_model_from_properties")
    void should_create_image_model_from_properties() {
        ImageModelConfig config = new ImageModelConfig();
        ImageProperties properties = new ImageProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://localhost:11434/v1/");
        properties.setModel("x/flux2-klein");

        ImageModel model = config.imageModel(properties);

        assertThat(model).isNotNull();
    }

    @Test
    @DisplayName("should_use_default_base_url_when_blank")
    void should_use_default_base_url_when_blank() {
        ImageModelConfig config = new ImageModelConfig();
        ImageProperties properties = new ImageProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("");

        ImageModel model = config.imageModel(properties);

        assertThat(model).isNotNull();
    }
}
