package com.ai.vision.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("YoloNonMaxSuppression")
class YoloNonMaxSuppressionTest {

  @Test
  @DisplayName("should suppress overlapping boxes")
  void shouldSuppressOverlappingBoxes() {
    var candidates =
        java.util.List.of(
            new com.ai.vision.domain.model.Detection("person", 0.9, 10, 10, 50, 50),
            new com.ai.vision.domain.model.Detection("person", 0.8, 12, 12, 48, 48),
            new com.ai.vision.domain.model.Detection("car", 0.7, 200, 200, 60, 40));

    var kept = YoloNonMaxSuppression.apply(candidates, 0.45f);

    assertThat(kept).hasSize(2);
    assertThat(kept.getFirst().className()).isEqualTo("person");
    assertThat(kept.get(1).className()).isEqualTo("car");
  }

  @Test
  @DisplayName("should use coco class names")
  void shouldUseCocoClassNames() {
    assertThat(CocoClassNames.label(0)).isEqualTo("person");
    assertThat(CocoClassNames.classCount()).isEqualTo(80);
  }
}
