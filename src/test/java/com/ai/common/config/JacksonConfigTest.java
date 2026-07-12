package com.ai.common.config;

import com.ai.common.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonConfig")
class JacksonConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JacksonConfig config = new JacksonConfig();
        objectMapper = config.objectMapper();
    }

    @Nested
    @DisplayName("objectMapper()")
    class ObjectMapperConfiguration {

        @Test
        @DisplayName("should have JavaTimeModule registered")
        void shouldHaveJavaTimeModuleRegistered() {
            assertThat(objectMapper.getRegisteredModuleIds()).contains("jackson-datatype-jsr310");
        }

        @Test
        @DisplayName("should serialize Instant as ISO-8601 string")
        void shouldSerializeInstantAsIso8601String() throws Exception {
            Instant instant = Instant.parse("2024-01-15T10:30:00Z");

            String json = objectMapper.writeValueAsString(new InstantWrapper(instant));

            assertThat(json).contains("2024-01-15T10:30:00Z");
            assertThat(json).doesNotContain("1705315800");
        }

        @Test
        @DisplayName("should deserialize ISO-8601 string to Instant")
        void shouldDeserializeIso8601StringToInstant() throws Exception {
            String json = "{\"instant\":\"2024-01-15T10:30:00Z\"}";

            InstantWrapper wrapper = objectMapper.readValue(json, InstantWrapper.class);

            assertThat(wrapper.instant).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
        }

        @Test
        @DisplayName("should be configured as primary")
        void shouldBeConfigurableAsPrimary() {
            assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        }
    }

    static class InstantWrapper {
        public Instant instant;

        public InstantWrapper() {
        }

        public InstantWrapper(Instant instant) {
            this.instant = instant;
        }
    }
}
