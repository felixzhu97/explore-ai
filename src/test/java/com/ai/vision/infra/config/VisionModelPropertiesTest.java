package com.ai.vision.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VisionModelProperties")
class VisionModelPropertiesTest {

  @Test
  @DisplayName("should expose nested configuration defaults")
  void shouldExposeNestedConfigurationDefaults() {
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
