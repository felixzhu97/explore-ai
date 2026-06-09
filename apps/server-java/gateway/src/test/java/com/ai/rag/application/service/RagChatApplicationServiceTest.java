package com.ai.rag.application.service;

import com.ai.rag.application.dto.ChatRequest;
import com.ai.rag.application.dto.ChatResponseDto;
import com.ai.rag.domain.SourceDocument;
import com.ai.rag.domain.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatApplicationService Tests")
class RagChatApplicationServiceTest {

    @Mock
    private VectorStore vectorStore;

    private RagChatApplicationService ragChatApplicationService;

    @BeforeEach
    void setUp() {
        ragChatApplicationService = new RagChatApplicationService(vectorStore);
    }

    @Nested
    @DisplayName("chat")
    class ChatTests {

        @Test
        @DisplayName("should return answer with sources when sources found")
        void shouldReturnAnswerWithSourcesWhenSourcesFound() {
            ChatRequest request = new ChatRequest(
                    "What is Java?", "session-123", 5, 0.7, null
            );
            List<SourceDocument> sources = List.of(
                    SourceDocument.of("Java is a programming language.", 0.95),
                    SourceDocument.of("Java runs on JVM.", 0.90)
            );

            when(vectorStore.searchWithScores(eq("What is Java?"), anyInt()))
                    .thenReturn(sources);

            ChatResponseDto response = ragChatApplicationService.chat(request);

            assertThat(response.answer()).contains("Based on the available information");
            assertThat(response.sessionId()).isEqualTo("session-123");
            assertThat(response.sources()).hasSize(2);
            assertThat(response.model()).isEqualTo("deepseek-v4-flash");
            assertThat(response.processingTimeMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should return empty sources message when no sources found")
        void shouldReturnEmptySourcesMessageWhenNoSourcesFound() {
            ChatRequest request = new ChatRequest(
                    "Unknown query", "session-456", 5, null, null
            );

            when(vectorStore.searchWithScores(anyString(), anyInt()))
                    .thenReturn(List.of());

            ChatResponseDto response = ragChatApplicationService.chat(request);

            assertThat(response.answer()).contains("don't have relevant information");
            assertThat(response.sessionId()).isEqualTo("session-456");
            assertThat(response.sources()).isEmpty();
        }

        @Test
        @DisplayName("should use default topK when not specified")
        void shouldUseDefaultTopKWhenNotSpecified() {
            ChatRequest request = new ChatRequest(
                    "Query", "session-789", null, null, null
            );

            when(vectorStore.searchWithScores(anyString(), eq(5)))
                    .thenReturn(List.of());

            ragChatApplicationService.chat(request);

            verify(vectorStore).searchWithScores("Query", 5);
        }

        @Test
        @DisplayName("should use custom topK when specified")
        void shouldUseCustomTopKWhenSpecified() {
            ChatRequest request = new ChatRequest(
                    "Query", "session-789", 10, null, null
            );

            when(vectorStore.searchWithScores(anyString(), eq(10)))
                    .thenReturn(List.of());

            ragChatApplicationService.chat(request);

            verify(vectorStore).searchWithScores("Query", 10);
        }

        @Test
        @DisplayName("should track processing time")
        void shouldTrackProcessingTime() {
            ChatRequest request = new ChatRequest(
                    "Query", "session-timer", 5, null, null
            );

            when(vectorStore.searchWithScores(anyString(), anyInt()))
                    .thenReturn(List.of());

            ChatResponseDto response = ragChatApplicationService.chat(request);

            assertThat(response.processingTimeMs()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("streamChat")
    class StreamChatTests {

        @Test
        @DisplayName("should stream answer with sources")
        void shouldStreamAnswerWithSources() {
            ChatRequest request = new ChatRequest(
                    "What is Spring?", "stream-session", 5, null, null
            );
            List<SourceDocument> sources = List.of(
                    SourceDocument.of("Spring is a framework.", 0.95)
            );

            when(vectorStore.searchWithScores(anyString(), anyInt()))
                    .thenReturn(sources);

            StepVerifier.create(ragChatApplicationService.streamChat(request))
                    .assertNext(chunk -> {
                        assertThat(chunk).contains("Based on the available information");
                        assertThat(chunk).contains("Spring is a framework");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should stream empty sources message when no results")
        void shouldStreamEmptySourcesMessageWhenNoResults() {
            ChatRequest request = new ChatRequest(
                    "No results query", "stream-empty", 5, null, null
            );

            when(vectorStore.searchWithScores(anyString(), anyInt()))
                    .thenReturn(List.of());

            StepVerifier.create(ragChatApplicationService.streamChat(request))
                    .assertNext(chunk -> {
                        assertThat(chunk).contains("don't have relevant information");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should emit single chunk then complete")
        void shouldEmitSingleChunkThenComplete() {
            ChatRequest request = new ChatRequest(
                    "Query", "single-chunk", 5, null, null
            );
            List<SourceDocument> sources = List.of(
                    SourceDocument.of("Answer content", 0.90)
            );

            when(vectorStore.searchWithScores(anyString(), anyInt()))
                    .thenReturn(sources);

            StepVerifier.create(ragChatApplicationService.streamChat(request))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should emit error on vector store failure")
        void shouldEmitErrorOnVectorStoreFailure() {
            ChatRequest request = new ChatRequest(
                    "Query", "error-session", 5, null, null
            );

            doThrow(new RuntimeException("Vector store error"))
                    .when(vectorStore).searchWithScores(anyString(), anyInt());

            StepVerifier.create(ragChatApplicationService.streamChat(request))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("searchSources")
    class SearchSourcesTests {

        @Test
        @DisplayName("should delegate to vector store")
        void shouldDelegateToVectorStore() {
            String query = "test query";
            int topK = 5;
            List<SourceDocument> expectedSources = List.of(
                    SourceDocument.of("Source 1", 0.9),
                    SourceDocument.of("Source 2", 0.8)
            );

            when(vectorStore.searchWithScores(query, topK))
                    .thenReturn(expectedSources);

            List<SourceDocument> result = ragChatApplicationService.searchSources(query, topK);

            assertThat(result).isEqualTo(expectedSources);
            verify(vectorStore).searchWithScores(query, topK);
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTests {

        @Test
        @DisplayName("should return vector store statistics")
        void shouldReturnVectorStoreStatistics() {
            java.util.Map<String, Object> expectedStats = java.util.Map.of(
                    "total_vectors", 1000,
                    "dimensions", 1536,
                    "index_type", "hnsw"
            );

            when(vectorStore.getStats()).thenReturn(expectedStats);

            java.util.Map<String, Object> result = ragChatApplicationService.getStats();

            assertThat(result).isEqualTo(expectedStats);
            verify(vectorStore).getStats();
        }

        @Test
        @DisplayName("should handle empty stats")
        void shouldHandleEmptyStats() {
            when(vectorStore.getStats()).thenReturn(java.util.Map.of());

            java.util.Map<String, Object> result = ragChatApplicationService.getStats();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("context building")
    class ContextBuildingTests {

        @Test
        @DisplayName("should concatenate sources with separator")
        void shouldConcatenateSourcesWithSeparator() {
            ChatRequest request = new ChatRequest(
                    "Multi-source query", "multi-source", 5, null, null
            );
            List<SourceDocument> sources = List.of(
                    SourceDocument.of("First source content", 0.95),
                    SourceDocument.of("Second source content", 0.90)
            );

            when(vectorStore.searchWithScores(anyString(), anyInt()))
                    .thenReturn(sources);

            ChatResponseDto response = ragChatApplicationService.chat(request);

            assertThat(response.answer()).contains("First source content");
            assertThat(response.answer()).contains("Second source content");
            assertThat(response.answer()).contains("---\n\n");
        }

        @Test
        @DisplayName("should handle single source")
        void shouldHandleSingleSource() {
            ChatRequest request = new ChatRequest(
                    "Single source query", "single-source", 5, null, null
            );
            List<SourceDocument> sources = List.of(
                    SourceDocument.of("Only source", 0.95)
            );

            when(vectorStore.searchWithScores(anyString(), anyInt()))
                    .thenReturn(sources);

            ChatResponseDto response = ragChatApplicationService.chat(request);

            assertThat(response.answer()).contains("Only source");
            assertThat(response.sources()).hasSize(1);
        }
    }
}
