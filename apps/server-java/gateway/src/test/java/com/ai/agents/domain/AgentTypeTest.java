package com.ai.agents.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentType Tests")
class AgentTypeTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("getId")
    class GetIdTests {

        @Test
        @DisplayName("should return chat id for CHAT type")
        void shouldReturnChatIdForChatType() {
            assertThat(AgentType.CHAT.getId()).isEqualTo("chat");
        }

        @Test
        @DisplayName("should return rag id for RAG type")
        void shouldReturnRagIdForRagType() {
            assertThat(AgentType.RAG.getId()).isEqualTo("rag");
        }

        @Test
        @DisplayName("should return tts id for TTS type")
        void shouldReturnTtsIdForTtsType() {
            assertThat(AgentType.TTS.getId()).isEqualTo("tts");
        }

        @Test
        @DisplayName("should return vision id for VISION type")
        void shouldReturnVisionIdForVisionType() {
            assertThat(AgentType.VISION.getId()).isEqualTo("vision");
        }

        @Test
        @DisplayName("should return media id for MEDIA type")
        void shouldReturnMediaIdForMediaType() {
            assertThat(AgentType.MEDIA.getId()).isEqualTo("media");
        }

        @Test
        @DisplayName("should return text id for TEXT type")
        void shouldReturnTextIdForTextType() {
            assertThat(AgentType.TEXT.getId()).isEqualTo("text");
        }

        @Test
        @DisplayName("should return supervisor id for SUPERVISOR type")
        void shouldReturnSupervisorIdForSupervisorType() {
            assertThat(AgentType.SUPERVISOR.getId()).isEqualTo("supervisor");
        }
    }

    @Nested
    @DisplayName("getDescription")
    class GetDescriptionTests {

        @Test
        @DisplayName("should return non-blank description for CHAT type")
        void shouldReturnNonBlankDescriptionForChatType() {
            assertThat(AgentType.CHAT.getDescription()).isNotBlank();
            assertThat(AgentType.CHAT.getDescription()).contains("chat");
        }

        @Test
        @DisplayName("should return non-blank description for RAG type")
        void shouldReturnNonBlankDescriptionForRagType() {
            assertThat(AgentType.RAG.getDescription()).isNotBlank();
            assertThat(AgentType.RAG.getDescription()).contains("Retrieval");
        }

        @Test
        @DisplayName("should return non-blank description for all types")
        void shouldReturnNonBlankDescriptionForAllTypes() {
            for (AgentType type : AgentType.values()) {
                assertThat(type.getDescription())
                        .as("Description for %s should not be blank", type)
                        .isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("fromId")
    class FromIdTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return CHAT for null or empty input")
        void shouldReturnChatForNullOrEmptyInput(String id) {
            assertThat(AgentType.fromId(id)).isEqualTo(AgentType.CHAT);
        }

        @Test
        @DisplayName("should return matching type for valid lowercase id")
        void shouldReturnMatchingTypeForValidLowercaseId() {
            assertThat(AgentType.fromId("chat")).isEqualTo(AgentType.CHAT);
            assertThat(AgentType.fromId("rag")).isEqualTo(AgentType.RAG);
            assertThat(AgentType.fromId("tts")).isEqualTo(AgentType.TTS);
            assertThat(AgentType.fromId("vision")).isEqualTo(AgentType.VISION);
            assertThat(AgentType.fromId("media")).isEqualTo(AgentType.MEDIA);
            assertThat(AgentType.fromId("text")).isEqualTo(AgentType.TEXT);
            assertThat(AgentType.fromId("supervisor")).isEqualTo(AgentType.SUPERVISOR);
        }

        @Test
        @DisplayName("should return matching type for valid uppercase id")
        void shouldReturnMatchingTypeForValidUppercaseId() {
            assertThat(AgentType.fromId("CHAT")).isEqualTo(AgentType.CHAT);
            assertThat(AgentType.fromId("RAG")).isEqualTo(AgentType.RAG);
            assertThat(AgentType.fromId("TTS")).isEqualTo(AgentType.TTS);
        }

        @Test
        @DisplayName("should return matching type for mixed case id")
        void shouldReturnMatchingTypeForMixedCaseId() {
            assertThat(AgentType.fromId("Chat")).isEqualTo(AgentType.CHAT);
            assertThat(AgentType.fromId("RaG")).isEqualTo(AgentType.RAG);
        }

        @Test
        @DisplayName("should return CHAT for unknown id")
        void shouldReturnChatForUnknownId() {
            assertThat(AgentType.fromId("unknown")).isEqualTo(AgentType.CHAT);
            assertThat(AgentType.fromId("invalid")).isEqualTo(AgentType.CHAT);
            assertThat(AgentType.fromId("xyz123")).isEqualTo(AgentType.CHAT);
        }
    }

    @Nested
    @DisplayName("fromOrdinal")
    class FromOrdinalTests {

        @Test
        @DisplayName("should return CHAT for negative ordinal")
        void shouldReturnChatForNegativeOrdinal() {
            assertThat(AgentType.fromOrdinal(-1)).isEqualTo(AgentType.CHAT);
            assertThat(AgentType.fromOrdinal(-100)).isEqualTo(AgentType.CHAT);
        }

        @Test
        @DisplayName("should return correct type for valid ordinal")
        void shouldReturnCorrectTypeForValidOrdinal() {
            AgentType[] types = AgentType.values();
            for (int i = 0; i < types.length; i++) {
                assertThat(AgentType.fromOrdinal(i)).isEqualTo(types[i]);
            }
        }

        @Test
        @DisplayName("should return CHAT for ordinal beyond array bounds")
        void shouldReturnChatForOrdinalBeyondBounds() {
            assertThat(AgentType.fromOrdinal(100)).isEqualTo(AgentType.CHAT);
            assertThat(AgentType.fromOrdinal(Integer.MAX_VALUE)).isEqualTo(AgentType.CHAT);
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerializationTests {

        @Test
        @DisplayName("should serialize to id value via JsonValue")
        void shouldSerializeToIdValueViaJsonValue() throws JsonProcessingException {
            String json = objectMapper.writeValueAsString(AgentType.RAG);
            assertThat(json).isEqualTo("\"rag\"");
        }

        @Test
        @DisplayName("should deserialize from id value via JsonCreator")
        void shouldDeserializeFromIdValueViaJsonCreator() throws JsonProcessingException {
            AgentType type = objectMapper.readValue("\"rag\"", AgentType.class);
            assertThat(type).isEqualTo(AgentType.RAG);
        }

        @Test
        @DisplayName("should handle case-insensitive deserialization")
        void shouldHandleCaseInsensitiveDeserialization() throws JsonProcessingException {
            assertThat(objectMapper.readValue("\"CHAT\"", AgentType.class)).isEqualTo(AgentType.CHAT);
            assertThat(objectMapper.readValue("\"Rag\"", AgentType.class)).isEqualTo(AgentType.RAG);
        }

        @ParameterizedTest
        @MethodSource("com.ai.agents.domain.AgentTypeTest#allTypesProvider")
        @DisplayName("should round-trip serialize and deserialize all types")
        void shouldRoundTripSerializeAndDeserializeAllTypes(AgentType type) throws JsonProcessingException {
            String json = objectMapper.writeValueAsString(type);
            AgentType deserialized = objectMapper.readValue(json, AgentType.class);
            assertThat(deserialized).isEqualTo(type);
        }
    }

    static Stream<Arguments> allTypesProvider() {
        return Stream.of(AgentType.values())
                .map(Arguments::of);
    }
}
