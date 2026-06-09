package com.ai.gateway.agent;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.RoutingDecision;
import com.ai.agents.domain.service.SupervisorAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SupervisorAgent Tests")
class SupervisorAgentTest {

    private SupervisorAgent supervisorAgent;

    @BeforeEach
    void setUp() {
        supervisorAgent = new SupervisorAgent();
    }

    @Nested
    @DisplayName("route")
    class RouteTests {

        @Test
        @DisplayName("should route RAG queries to RAG agent")
        void shouldRouteRagQueriesToRagAgent() {
            String query = "What is RAG?";

            RoutingDecision result = supervisorAgent.route(query);

            assertThat(result).isNotNull();
            assertThat(result.targetType()).isEqualTo(AgentType.RAG);
        }

        @Test
        @DisplayName("should route queries with document keywords to RAG")
        void shouldRouteQueriesWithDocumentKeywordsToRag() {
            RoutingDecision result = supervisorAgent.route("search for document");

            assertThat(result.targetType()).isEqualTo(AgentType.RAG);
        }

        @Test
        @DisplayName("should route queries with knowledge base keywords to RAG")
        void shouldRouteQueriesWithKnowledgeBaseKeywordsToRag() {
            RoutingDecision result = supervisorAgent.route("search knowledge base");

            assertThat(result.targetType()).isEqualTo(AgentType.RAG);
        }

        @Test
        @DisplayName("should route TTS queries to TTS agent")
        void shouldRouteTtsQueriesToTtsAgent() {
            RoutingDecision result = supervisorAgent.route("convert text to speech");

            assertThat(result.targetType()).isEqualTo(AgentType.TTS);
        }

        @Test
        @DisplayName("should route queries with audio keywords to TTS")
        void shouldRouteQueriesWithAudioKeywordsToTts() {
            RoutingDecision result = supervisorAgent.route("generate audio from text");

            assertThat(result.targetType()).isEqualTo(AgentType.TTS);
        }

        @Test
        @DisplayName("should route vision queries to VISION agent")
        void shouldRouteVisionQueriesToVisionAgent() {
            RoutingDecision result = supervisorAgent.route("analyze this image");

            assertThat(result.targetType()).isEqualTo(AgentType.VISION);
        }

        @Test
        @DisplayName("should route media queries to MEDIA agent")
        void shouldRouteMediaQueriesToMediaAgent() {
            RoutingDecision result = supervisorAgent.route("draw a cat");

            assertThat(result.targetType()).isEqualTo(AgentType.MEDIA);
        }

        @Test
        @DisplayName("should route translation queries to TEXT agent")
        void shouldRouteTranslationQueriesToTextAgent() {
            RoutingDecision result = supervisorAgent.route("translate this to English");

            assertThat(result.targetType()).isEqualTo(AgentType.TEXT);
        }

        @Test
        @DisplayName("should default to CHAT for unrecognized queries")
        void shouldDefaultToChatForUnrecognizedQueries() {
            RoutingDecision result = supervisorAgent.route("Hello, how are you?");

            assertThat(result.targetType()).isEqualTo(AgentType.CHAT);
        }

        @Test
        @DisplayName("should return fallback for null message")
        void shouldReturnFallbackForNullMessage() {
            RoutingDecision result = supervisorAgent.route(null);

            assertThat(result.targetType()).isEqualTo(AgentType.CHAT);
        }

        @Test
        @DisplayName("should return fallback for blank message")
        void shouldReturnFallbackForBlankMessage() {
            RoutingDecision result = supervisorAgent.route("   ");

            assertThat(result.targetType()).isEqualTo(AgentType.CHAT);
        }
    }

    @Nested
    @DisplayName("routeTo")
    class RouteToTests {

        @Test
        @DisplayName("should route with explicit type and keyword match")
        void shouldRouteWithExplicitTypeAndKeywordMatch() {
            RoutingDecision result = supervisorAgent.routeTo(AgentType.RAG, "search document");

            assertThat(result.targetType()).isEqualTo(AgentType.RAG);
            assertThat(result.confidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should route to specified type when no keyword match")
        void shouldRouteToSpecifiedTypeWhenNoKeywordMatch() {
            RoutingDecision result = supervisorAgent.routeTo(AgentType.TTS, "hello world");

            assertThat(result.targetType()).isEqualTo(AgentType.TTS);
        }

        @Test
        @DisplayName("should default to CHAT when type is null")
        void shouldDefaultToChatWhenTypeIsNull() {
            RoutingDecision result = supervisorAgent.routeTo(null, "some message");

            assertThat(result.targetType()).isEqualTo(AgentType.CHAT);
        }
    }

    @Nested
    @DisplayName("getKeywordsForType")
    class GetKeywordsForTypeTests {

        @Test
        @DisplayName("should return keywords for RAG type")
        void shouldReturnKeywordsForRagType() {
            Set<String> keywords = supervisorAgent.getKeywordsForType(AgentType.RAG);

            assertThat(keywords).isNotEmpty();
            assertThat(keywords).contains("rag", "search", "document");
        }

        @Test
        @DisplayName("should return empty set for unknown type")
        void shouldReturnEmptySetForUnknownType() {
            Set<String> keywords = supervisorAgent.getKeywordsForType(null);

            assertThat(keywords).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllKeywords")
    class GetAllKeywordsTests {

        @Test
        @DisplayName("should return all routing keywords")
        void shouldReturnAllRoutingKeywords() {
            Map<AgentType, Set<String>> allKeywords = supervisorAgent.getAllKeywords();

            assertThat(allKeywords).containsKey(AgentType.RAG);
            assertThat(allKeywords).containsKey(AgentType.TTS);
            assertThat(allKeywords).containsKey(AgentType.VISION);
            assertThat(allKeywords).containsKey(AgentType.MEDIA);
            assertThat(allKeywords).containsKey(AgentType.TEXT);
        }
    }
}
