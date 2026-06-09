package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentCapabilities Tests")
class AgentCapabilitiesTest {

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("should create RAG capabilities")
        void shouldCreateRagCapabilities() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.RAG);

            assertThat(capabilities.supportsRag()).isTrue();
            assertThat(capabilities.supportsTts()).isFalse();
            assertThat(capabilities.supportsVision()).isFalse();
            assertThat(capabilities.supportsMedia()).isFalse();
            assertThat(capabilities.supportsText()).isFalse();
            assertThat(capabilities.supportsChat()).isFalse();
        }

        @Test
        @DisplayName("should create TTS capabilities")
        void shouldCreateTtsCapabilities() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.TTS);

            assertThat(capabilities.supportsRag()).isFalse();
            assertThat(capabilities.supportsTts()).isTrue();
            assertThat(capabilities.supportsVision()).isFalse();
            assertThat(capabilities.supportsMedia()).isFalse();
            assertThat(capabilities.supportsText()).isFalse();
            assertThat(capabilities.supportsChat()).isFalse();
        }

        @Test
        @DisplayName("should create Vision capabilities")
        void shouldCreateVisionCapabilities() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.VISION);

            assertThat(capabilities.supportsRag()).isFalse();
            assertThat(capabilities.supportsTts()).isFalse();
            assertThat(capabilities.supportsVision()).isTrue();
            assertThat(capabilities.supportsMedia()).isFalse();
            assertThat(capabilities.supportsText()).isFalse();
            assertThat(capabilities.supportsChat()).isFalse();
        }

        @Test
        @DisplayName("should create Media capabilities")
        void shouldCreateMediaCapabilities() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.MEDIA);

            assertThat(capabilities.supportsRag()).isFalse();
            assertThat(capabilities.supportsTts()).isFalse();
            assertThat(capabilities.supportsVision()).isFalse();
            assertThat(capabilities.supportsMedia()).isTrue();
            assertThat(capabilities.supportsText()).isFalse();
            assertThat(capabilities.supportsChat()).isFalse();
        }

        @Test
        @DisplayName("should create Text capabilities")
        void shouldCreateTextCapabilities() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.TEXT);

            assertThat(capabilities.supportsRag()).isFalse();
            assertThat(capabilities.supportsTts()).isFalse();
            assertThat(capabilities.supportsVision()).isFalse();
            assertThat(capabilities.supportsMedia()).isFalse();
            assertThat(capabilities.supportsText()).isTrue();
            assertThat(capabilities.supportsChat()).isFalse();
        }

        @Test
        @DisplayName("should create Chat capabilities")
        void shouldCreateChatCapabilities() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.CHAT);

            assertThat(capabilities.supportsRag()).isFalse();
            assertThat(capabilities.supportsTts()).isFalse();
            assertThat(capabilities.supportsVision()).isFalse();
            assertThat(capabilities.supportsMedia()).isFalse();
            assertThat(capabilities.supportsText()).isFalse();
            assertThat(capabilities.supportsChat()).isTrue();
        }

        @Test
        @DisplayName("should create Supervisor capabilities with all supports")
        void shouldCreateSupervisorCapabilitiesWithAllSupports() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.SUPERVISOR);

            assertThat(capabilities.supportsRag()).isTrue();
            assertThat(capabilities.supportsTts()).isTrue();
            assertThat(capabilities.supportsVision()).isTrue();
            assertThat(capabilities.supportsMedia()).isTrue();
            assertThat(capabilities.supportsText()).isTrue();
            assertThat(capabilities.supportsChat()).isTrue();
        }
    }

    @Nested
    @DisplayName("all factory method")
    class AllFactoryMethodTests {

        @Test
        @DisplayName("should create capabilities with all supports enabled")
        void shouldCreateCapabilitiesWithAllSupportsEnabled() {
            AgentCapabilities capabilities = AgentCapabilities.all();

            assertThat(capabilities.supportsRag()).isTrue();
            assertThat(capabilities.supportsTts()).isTrue();
            assertThat(capabilities.supportsVision()).isTrue();
            assertThat(capabilities.supportsMedia()).isTrue();
            assertThat(capabilities.supportsText()).isTrue();
            assertThat(capabilities.supportsChat()).isTrue();
        }
    }

    @Nested
    @DisplayName("none factory method")
    class NoneFactoryMethodTests {

        @Test
        @DisplayName("should create capabilities with all supports disabled")
        void shouldCreateCapabilitiesWithAllSupportsDisabled() {
            AgentCapabilities capabilities = AgentCapabilities.none();

            assertThat(capabilities.supportsRag()).isFalse();
            assertThat(capabilities.supportsTts()).isFalse();
            assertThat(capabilities.supportsVision()).isFalse();
            assertThat(capabilities.supportsMedia()).isFalse();
            assertThat(capabilities.supportsText()).isFalse();
            assertThat(capabilities.supportsChat()).isFalse();
        }
    }

    @Nested
    @DisplayName("supports method")
    class SupportsMethodTests {

        @ParameterizedTest
        @EnumSource(AgentType.class)
        @DisplayName("should correctly report support for each type")
        void shouldCorrectlyReportSupportForEachType(AgentType type) {
            AgentCapabilities capabilities = AgentCapabilities.of(type);

            assertThat(capabilities.supports(type)).isTrue();
        }

        @Test
        @DisplayName("should not support other types for RAG capability")
        void shouldNotSupportOtherTypesForRagCapability() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.RAG);

            assertThat(capabilities.supports(AgentType.TTS)).isFalse();
            assertThat(capabilities.supports(AgentType.VISION)).isFalse();
            assertThat(capabilities.supports(AgentType.MEDIA)).isFalse();
            assertThat(capabilities.supports(AgentType.TEXT)).isFalse();
            assertThat(capabilities.supports(AgentType.CHAT)).isFalse();
        }

        @Test
        @DisplayName("should support all types for supervisor capability")
        void shouldSupportAllTypesForSupervisorCapability() {
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.SUPERVISOR);

            for (AgentType type : AgentType.values()) {
                assertThat(capabilities.supports(type)).isTrue();
            }
        }

        @Test
        @DisplayName("should support all types for all() capability")
        void shouldSupportAllTypesForAllCapability() {
            AgentCapabilities capabilities = AgentCapabilities.all();

            for (AgentType type : AgentType.values()) {
                assertThat(capabilities.supports(type)).isTrue();
            }
        }

        @Test
        @DisplayName("should not support any types for none() capability")
        void shouldNotSupportAnyTypesForNoneCapability() {
            AgentCapabilities capabilities = AgentCapabilities.none();

            for (AgentType type : AgentType.values()) {
                assertThat(capabilities.supports(type)).isFalse();
            }
        }
    }
}
