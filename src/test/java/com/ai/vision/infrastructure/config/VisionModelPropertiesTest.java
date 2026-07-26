package com.ai.vision.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VisionModelProperties")
class VisionModelPropertiesTest {

    @Test
    @DisplayName("should_expose_nested_configuration_defaults")
    void should_expose_nested_configuration_defaults() {
        VisionModelProperties properties = new VisionModelProperties();

        assertThat(properties.getModelsDir()).isEqualTo("models");
        assertThat(properties.getDetect().getInputSize()).isEqualTo(640);
        assertThat(properties.getCaption().getMaxLength()).isEqualTo(50);
        assertThat(properties.getOcr().getLanguages()).isEqualTo("eng+chi_sim");

        properties.setModelsDir("custom-models");
        properties.getDetect().setConfidenceThreshold(0.7f);
        properties.getCaption().setVisionOnnx("vision.onnx");
        properties.getOcr().setPageSegMode(6);

        assertThat(properties.getModelsDir()).isEqualTo("custom-models");
        assertThat(properties.getDetect().getConfidenceThreshold()).isEqualTo(0.7f);
        assertThat(properties.getCaption().getVisionOnnx()).isEqualTo("vision.onnx");
        assertThat(properties.getOcr().getPageSegMode()).isEqualTo(6);
    }
}
