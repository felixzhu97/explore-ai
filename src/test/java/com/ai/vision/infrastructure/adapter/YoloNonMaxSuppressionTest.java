package com.ai.vision.infrastructure.adapter;

import com.ai.vision.infrastructure.config.VisionModelProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YoloNonMaxSuppression")
class YoloNonMaxSuppressionTest {

    @Test
    @DisplayName("should_suppress_overlapping_boxes")
    void should_suppress_overlapping_boxes() {
        var candidates = java.util.List.of(
                new com.ai.vision.domain.model.Detection("person", 0.9, 10, 10, 50, 50),
                new com.ai.vision.domain.model.Detection("person", 0.8, 12, 12, 48, 48),
                new com.ai.vision.domain.model.Detection("car", 0.7, 200, 200, 60, 40));

        var kept = YoloNonMaxSuppression.apply(candidates, 0.45f);

        assertThat(kept).hasSize(2);
        assertThat(kept.getFirst().className()).isEqualTo("person");
        assertThat(kept.get(1).className()).isEqualTo("car");
    }

    @Test
    @DisplayName("should_use_coco_class_names")
    void should_use_coco_class_names() {
        assertThat(CocoClassNames.label(0)).isEqualTo("person");
        assertThat(CocoClassNames.classCount()).isEqualTo(80);
    }
}
