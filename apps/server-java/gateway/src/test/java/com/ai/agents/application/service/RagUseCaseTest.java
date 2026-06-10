package com.ai.agents.application.service;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.service.agents.RagAgentService;
import com.ai.agents.domain.workflow.RAGWorkflow;
import com.ai.agents.presentation.dto.AgentResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagUseCase Tests")
class RagUseCaseTest {

    @Mock
    private RagAgentService domainService;

    private RagUseCase ragUseCase;

    @BeforeEach
    void setUp() {
        ragUseCase = new RagUseCase(domainService);
    }

    @Nested
    @DisplayName("indexDocument")
    class IndexDocumentTests {

        @Test
        @DisplayName("should return success response when document indexed successfully")
        void shouldReturnSuccessWhenIndexed() {
            String content = "This is a test document about Java programming.";
            String title = "Java Guide";
            Map<String, String> metadata = Map.of("author", "test");
            RagAgentService.Document indexedDoc = new RagAgentService.Document(
                    "doc-123", title, content, metadata,
                    java.time.Instant.now(), List.of("chunk1", "chunk2")
            );

            when(domainService.indexDocument(content, title, metadata)).thenReturn(indexedDoc);

            StepVerifier.create(ragUseCase.indexDocument(content, title, metadata))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("Document indexed successfully");
                        assertThat(response.message()).contains("doc-123");
                        assertThat(response.message()).contains(title);
                        assertThat(response.agentType()).isEqualTo(AgentType.RAG);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle null metadata")
        void shouldHandleNullMetadata() {
            String content = "Document content";
            String title = "Test Doc";
            RagAgentService.Document indexedDoc = new RagAgentService.Document(
                    "doc-456", title, content, Map.of(),
                    java.time.Instant.now(), List.of("chunk1")
            );

            when(domainService.indexDocument(content, title, null)).thenReturn(indexedDoc);

            StepVerifier.create(ragUseCase.indexDocument(content, title, null))
                    .assertNext(response -> {
                        assertThat(response.agentType()).isEqualTo(AgentType.RAG);
                        assertThat(response.error()).isNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error response when indexing fails")
        void shouldReturnErrorWhenIndexingFails() {
            String content = "Content";
            String title = "Failing Doc";
            String errorMessage = "Vector store connection failed";

            when(domainService.indexDocument(content, title, Map.of()))
                    .thenThrow(new RuntimeException(errorMessage));

            StepVerifier.create(ragUseCase.indexDocument(content, title, Map.of()))
                    .assertNext(response -> {
                        assertThat(response.error()).contains(errorMessage);
                        assertThat(response.agentType()).isNull();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("should return search results when found")
        void shouldReturnSearchResults() {
            String query = "Java programming";
            int topK = 5;
            List<RagAgentService.SearchResult> searchResults = List.of(
                    new RagAgentService.SearchResult(
                            "doc-1", "Java Guide",
                            "Java is a programming language created by James Gosling.",
                            0.95, 0
                    ),
                    new RagAgentService.SearchResult(
                            "doc-2", "Java Tutorial",
                            "This tutorial covers Java basics.",
                            0.85, 1
                    )
            );

            when(domainService.search(query, topK)).thenReturn(searchResults);

            StepVerifier.create(ragUseCase.search(query, topK))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("Search Results");
                        assertThat(response.message()).contains("Java Guide");
                        assertThat(response.message()).contains("Java Tutorial");
                        assertThat(response.agentType()).isEqualTo(AgentType.RAG);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle empty search results")
        void shouldHandleEmptySearchResults() {
            String query = "nonexistent";
            int topK = 5;

            when(domainService.search(query, topK)).thenReturn(List.of());

            StepVerifier.create(ragUseCase.search(query, topK))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("Search Results");
                        assertThat(response.agentType()).isEqualTo(AgentType.RAG);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle search error")
        void shouldHandleSearchError() {
            String query = "test query";
            int topK = 5;

            when(domainService.search(query, topK))
                    .thenThrow(new RuntimeException("Search engine unavailable"));

            StepVerifier.create(ragUseCase.search(query, topK))
                    .assertNext(response -> {
                        assertThat(response.error()).contains("Search engine unavailable");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should truncate long chunks in results")
        void shouldTruncateLongChunks() {
            String query = "test";
            int topK = 1;
            String longChunk = "a".repeat(500);
            List<RagAgentService.SearchResult> searchResults = List.of(
                    new RagAgentService.SearchResult("doc-1", "Long Doc", longChunk, 0.9, 0)
            );

            when(domainService.search(query, topK)).thenReturn(searchResults);

            StepVerifier.create(ragUseCase.search(query, topK))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("Long Doc");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("multiHopSearch")
    class MultiHopSearchTests {

        @Test
        @DisplayName("should complete multi-hop search successfully")
        void shouldCompleteMultiHopSearch() {
            String query = "complex question";
            int hops = 3;

            StepVerifier.create(ragUseCase.multiHopSearch(query, hops))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("Multi-hop Search Completed");
                        assertThat(response.message()).contains("Hops: 3");
                        assertThat(response.agentType()).isEqualTo(AgentType.RAG);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle single hop")
        void shouldHandleSingleHop() {
            String query = "simple question";
            int hops = 1;

            StepVerifier.create(ragUseCase.multiHopSearch(query, hops))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("Hops: 1");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle multi-hop search error")
        void shouldHandleMultiHopSearchError() {
            String query = "failing query";
            int hops = 5;

            when(domainService.search(eq("failing query"), anyInt()))
                    .thenThrow(new RuntimeException("Hop timeout"));

            StepVerifier.create(ragUseCase.multiHopSearch(query, hops))
                    .assertNext(response -> {
                        assertThat(response.error()).contains("Hop timeout");
                    })
                    .verifyComplete();
        }
    }
}
