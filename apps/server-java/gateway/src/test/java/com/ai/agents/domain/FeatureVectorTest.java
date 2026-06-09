package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeatureVector Tests")
class FeatureVectorTest {

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create feature vector with all parameters")
        void shouldCreateFeatureVectorWithAllParameters() {
            Map<String, Object> features = Map.of("temperature", 25.5, "humidity", 0.6);

            FeatureVector vector = FeatureVector.create("weather", "sensor-001", features);

            assertThat(vector.featureName()).isEqualTo("weather");
            assertThat(vector.entityId()).isEqualTo("sensor-001");
            assertThat(vector.features()).containsEntry("temperature", 25.5);
            assertThat(vector.features()).containsEntry("humidity", 0.6);
        }

        @Test
        @DisplayName("should set timestamp to now")
        void shouldSetTimestampToNow() {
            FeatureVector vector = FeatureVector.create("test", "entity-1", Map.of());

            assertThat(vector.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should initialize with empty metadata")
        void shouldInitializeWithEmptyMetadata() {
            FeatureVector vector = FeatureVector.create("test", "entity-1", Map.of());

            assertThat(vector.metadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("withMetadata method")
    class WithMetadataMethodTests {

        @Test
        @DisplayName("should add metadata to feature vector")
        void shouldAddMetadataToFeatureVector() {
            FeatureVector vector = FeatureVector.create("test", "entity-1", Map.of());

            FeatureVector withMeta = vector.withMetadata("source", "api");

            assertThat(withMeta.metadata()).containsEntry("source", "api");
        }

        @Test
        @DisplayName("should not modify original vector")
        void shouldNotModifyOriginalVector() {
            FeatureVector vector = FeatureVector.create("test", "entity-1", Map.of());

            vector.withMetadata("key", "value");

            assertThat(vector.metadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFeature method")
    class GetFeatureMethodTests {

        @Test
        @DisplayName("should return feature value by key")
        void shouldReturnFeatureValueByKey() {
            Map<String, Object> features = Map.of("count", 42);
            FeatureVector vector = FeatureVector.create("test", "entity-1", features);

            assertThat(vector.getFeature("count")).isEqualTo(42);
        }

        @Test
        @DisplayName("should return null for non-existent key")
        void shouldReturnNullForNonExistentKey() {
            FeatureVector vector = FeatureVector.create("test", "entity-1", Map.of());

            assertThat(vector.getFeature("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("getMetadata method")
    class GetMetadataMethodTests {

        @Test
        @DisplayName("should return metadata value by key")
        void shouldReturnMetadataValueByKey() {
            FeatureVector vector = FeatureVector.create("test", "entity-1", Map.of())
                    .withMetadata("version", "1.0");

            assertThat(vector.getMetadata("version")).isEqualTo("1.0");
        }
    }
}
