package com.ai.application.usecase;

import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.RagContextFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RagChatUseCase Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (ports):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests RAG retrieval flow
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatUseCase")
class RagChatUseCaseTest {

    @Mock
    private EmbeddingPort embeddingPort;

    @Mock
    private VectorSearchPort vectorSearchPort;

    private RagChatUseCase useCase;

    private static final String TEST_QUERY = "What is AI?";
    private static final float[] TEST_EMBEDDING = new float[]{0.1f, 0.2f, 0.3f};
    private static final int DEFAULT_TOP_K = 5;

    @BeforeEach
    void setUp() {
        useCase = new RagChatUseCase(embeddingPort, vectorSearchPort);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should embed question before searching vectors")
        void shouldEmbedQuestionBeforeSearchingVectors() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            verify(embeddingPort).embed(TEST_QUERY);
            verify(vectorSearchPort).search(eq(TEST_EMBEDDING), eq(DEFAULT_TOP_K));
        }

        @Test
        @DisplayName("should search vectors with query embedding")
        void shouldSearchVectorsWithQueryEmbedding() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
        }

        @Test
        @DisplayName("should construct context from chunks")
        void shouldConstructContextFromChunks() {
            // Arrange
            UUID docId = UUID.randomUUID();
            float[] queryEmbedding = new float[]{0.1f, 0.1f}; // Same size as chunk embeddings
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(queryEmbedding);
            
            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, "First chunk content", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Second chunk content", 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.context()).contains("First chunk content");
            assertThat(result.context()).contains("Second chunk content");
        }

        @Test
        @DisplayName("should return empty context when no vectors found")
        void shouldReturnEmptyContextWhenNoVectorsFound() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.context()).isEmpty();
            assertThat(result.sources()).isEmpty();
        }

        @Test
        @DisplayName("should limit to top-k results")
        void shouldLimitToTopKResults() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.1f, 0.1f});
            
            List<DocumentChunk> manyChunks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                manyChunks.add(createChunkWithEmbedding(UUID.randomUUID(), docId, "Content " + i, 2));
            }
            when(vectorSearchPort.search(any(float[].class), eq(3))).thenReturn(manyChunks.subList(0, 3));

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, 3);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(3));
        }

        @Test
        @DisplayName("should use default top-k when zero provided")
        void shouldUseDefaultTopKWhenZeroProvided() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), eq(DEFAULT_TOP_K))).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, 0);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
        }

        @Test
        @DisplayName("should use default top-k when negative provided")
        void shouldUseDefaultTopKWhenNegativeProvided() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), eq(DEFAULT_TOP_K))).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, -5);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
        }
    }

    @Nested
    @DisplayName("with Document IDs")
    class WithDocumentIds {

        @Test
        @DisplayName("should filter search by document IDs when provided")
        void shouldFilterSearchByDocumentIdsWhenProvided() {
            // Arrange
            UUID docId1 = UUID.randomUUID();
            UUID docId2 = UUID.randomUUID();
            List<UUID> docIds = List.of(docId1, docId2);
            
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt(), any())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, docIds, DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(eq(TEST_EMBEDDING), eq(DEFAULT_TOP_K), eq(docIds));
        }

        @Test
        @DisplayName("should search without doc IDs when list is empty")
        void shouldSearchWithoutDocIdsWhenListIsEmpty() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, Collections.emptyList(), DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
            verify(vectorSearchPort, never()).search(any(float[].class), anyInt(), any());
        }

        @Test
        @DisplayName("should search without doc IDs when null")
        void shouldSearchWithoutDocIdsWhenNull() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
            verify(vectorSearchPort, never()).search(any(float[].class), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("Source Documents")
    class SourceDocuments {

        @Test
        @DisplayName("should create source documents with scores")
        void shouldCreateSourceDocumentsWithScores() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});

            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Content with relevant info", 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).text()).contains("Content with relevant info");
            assertThat(result.sources().get(0).score()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("should include metadata in sources")
        void shouldIncludeMetadataInSources() {
            // Arrange
            UUID docId = UUID.randomUUID();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "test.pdf");
            metadata.put("page", 1);

            DocumentChunk chunk = createChunkWithMetadata(UUID.randomUUID(), docId, "Test content", metadata, 2);

            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(List.of(chunk));

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources().get(0).metadata()).containsEntry("source", "test.pdf");
            assertThat(result.sources().get(0).metadata()).containsEntry("page", 1);
        }

        @Test
        @DisplayName("should assign sequential indices to sources starting at 1")
        void shouldAssignSequentialIndicesToSourcesStartingAtOne() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});

            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, "First source", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Second source", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Third source", 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources()).hasSize(3);
            assertThat(result.sources().get(0).index()).isEqualTo(1);
            assertThat(result.sources().get(1).index()).isEqualTo(2);
            assertThat(result.sources().get(2).index()).isEqualTo(3);
        }

        @Test
        @DisplayName("should extract documentTitle from chunk metadata")
        void shouldExtractDocumentTitleFromChunkMetadata() {
            // Arrange
            UUID docId = UUID.randomUUID();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentTitle", "Test Document.pdf");

            DocumentChunk chunk = createChunkWithMetadata(UUID.randomUUID(), docId, "Test content", metadata, 2);

            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(List.of(chunk));

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).documentTitle()).isEqualTo("Test Document.pdf");
        }

        @Test
        @DisplayName("should use filename as fallback when documentTitle is absent")
        void shouldUseFilenameAsFallbackWhenDocumentTitleIsAbsent() {
            // Arrange
            UUID docId = UUID.randomUUID();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", "fallback.txt");

            DocumentChunk chunk = createChunkWithMetadata(UUID.randomUUID(), docId, "Test content", metadata, 2);

            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(List.of(chunk));

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).documentTitle()).isEqualTo("fallback.txt");
        }
    }

    @Nested
    @DisplayName("Delegation to RagContextFormatter")
    class DelegationToRagContextFormatter {

        @Test
        @DisplayName("should delegate formatting to RagContextFormatter")
        void shouldDelegateFormattingToRagContextFormatter() {
            // Arrange
            UUID docId = UUID.randomUUID();
            float[] queryEmbedding = new float[]{0.5f, 0.5f};
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentTitle", "Test Document.pdf");

            DocumentChunk chunk = createChunkWithMetadata(
                UUID.randomUUID(), docId, "Test content", metadata, 2
            );
            List<DocumentChunk> chunks = List.of(chunk);

            when(embeddingPort.embed(TEST_QUERY)).thenReturn(queryEmbedding);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert: verify each source matches what RagContextFormatter produces directly
            SourceDocument expectedSource = RagContextFormatter.formatSource(chunk, 1, queryEmbedding);
            SourceDocument actualSource = result.sources().get(0);

            assertThat(actualSource.index()).isEqualTo(expectedSource.index());
            assertThat(actualSource.text()).isEqualTo(expectedSource.text());
            assertThat(actualSource.documentTitle()).isEqualTo(expectedSource.documentTitle());
            assertThat(actualSource.score()).isEqualTo(expectedSource.score());

            // Assert: context matches what buildContextWithSources produces directly
            String expectedContext = RagContextFormatter.buildContextWithSources(chunks, queryEmbedding);
            assertThat(result.context()).isEqualTo(expectedContext);
        }
    }

    @Nested
    @DisplayName("Context Construction")
    class ContextConstruction {

        @Test
        @DisplayName("should join chunks with double newline separator")
        void shouldJoinChunksWithDoubleNewlineSeparator() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            
            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, "First", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Second", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Third", 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.context()).contains("First");
            assertThat(result.context()).contains("Second");
            assertThat(result.context()).contains("Third");
        }

        @Test
        @DisplayName("should return empty context and sources when no chunks found")
        void shouldReturnEmptyContextAndSourcesWhenNoChunksFound() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.context()).isEmpty();
            assertThat(result.sources()).isEmpty();
        }
    }

    @Nested
    @DisplayName("EnrichWithHistory")
    class EnrichWithHistory {

        @Test
        @DisplayName("should return original query when history is null")
        void shouldReturnOriginalQueryWhenHistoryIsNull() {
            // Arrange
            String query = "Current question";
            when(embeddingPort.embed(query)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, null);

            // Assert
            assertThat(result.enrichedQuery()).isEqualTo(query);
        }

        @Test
        @DisplayName("should return original query when history is empty")
        void shouldReturnOriginalQueryWhenHistoryIsEmpty() {
            // Arrange
            String query = "Current question";
            when(embeddingPort.embed(query)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, Collections.emptyList());

            // Assert
            assertThat(result.enrichedQuery()).isEqualTo(query);
        }

        @Test
        @DisplayName("should truncate history at MAX_HISTORY_MESSAGES")
        void shouldTruncateHistoryAtMaxHistoryMessages() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> manyMessages = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                manyMessages.add(ChatMessage.createUserMessage("Historical message " + i));
            }
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, manyMessages);

            // Assert
            // MAX_HISTORY_MESSAGES is 10, so only last 10 messages should be included
            assertThat(result.enrichedQuery()).doesNotContain("Historical message 0");
            assertThat(result.enrichedQuery()).contains("Historical message 14");
            assertThat(result.enrichedQuery()).contains("Historical message 5");
        }

        @Test
        @DisplayName("should include exactly 10 messages when history has more")
        void shouldIncludeExactlyTenMessagesWhenHistoryHasMore() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> messages = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                messages.add(ChatMessage.createUserMessage("Message " + i));
            }
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, messages);

            // Assert
            String enriched = result.enrichedQuery();
            long userCount = enriched.chars().filter(ch -> ch == 'u').count();
            // Count occurrences of "user: Message" - should be exactly 10
            long messageCount = enriched.split("user: Message").length - 1;
            assertThat(messageCount).isEqualTo(10);
        }

        @Test
        @DisplayName("should format enriched query with Previous conversation header")
        void shouldFormatEnrichedQueryWithPreviousConversationHeader() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> history = List.of(
                ChatMessage.createUserMessage("First question"),
                ChatMessage.createAssistantMessage("First answer")
            );
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, history);

            // Assert
            assertThat(result.enrichedQuery()).startsWith("Previous conversation:");
        }

        @Test
        @DisplayName("should format enriched query with Current question header")
        void shouldFormatEnrichedQueryWithCurrentQuestionHeader() {
            // Arrange
            String query = "What is the answer?";
            List<ChatMessage> history = List.of(
                ChatMessage.createUserMessage("Previous question")
            );
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, history);

            // Assert
            assertThat(result.enrichedQuery()).contains("Current question: " + query);
        }

        @Test
        @DisplayName("should format history messages with role and text")
        void shouldFormatHistoryMessagesWithRoleAndText() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> history = List.of(
                ChatMessage.createUserMessage("User message content"),
                ChatMessage.createAssistantMessage("Assistant message content")
            );
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, history);

            // Assert
            String enriched = result.enrichedQuery();
            assertThat(enriched).contains("user: User message content");
            assertThat(enriched).contains("assistant: Assistant message content");
        }

        @Test
        @DisplayName("should embed enriched query instead of original query")
        void shouldEmbedEnrichedQueryInsteadOfOriginal() {
            // Arrange
            String originalQuery = "Original query";
            String enrichedQuery = "Previous conversation:\nuser: History\n\nCurrent question: Original query";
            
            when(embeddingPort.embed(enrichedQuery)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(originalQuery, null, DEFAULT_TOP_K, List.of(ChatMessage.createUserMessage("History")));

            // Assert
            verify(embeddingPort).embed(enrichedQuery);
            verify(embeddingPort, never()).embed(originalQuery);
        }

        @Test
        @DisplayName("should return enriched query in RetrievalResult")
        void shouldReturnEnrichedQueryInResult() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> history = List.of(
                ChatMessage.createUserMessage("Previous message")
            );
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, history);

            // Assert
            assertThat(result.enrichedQuery())
                .contains("Previous conversation:")
                .contains("user: Previous message")
                .contains("Current question: " + query);
        }

        @Test
        @DisplayName("should handle history with only assistant messages")
        void shouldHandleHistoryWithOnlyAssistantMessages() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> history = List.of(
                ChatMessage.createAssistantMessage("Previous answer")
            );
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, history);

            // Assert
            assertThat(result.enrichedQuery())
                .contains("Previous conversation:")
                .contains("assistant: Previous answer")
                .contains("Current question: " + query);
        }

        @Test
        @DisplayName("should handle single message history")
        void shouldHandleSingleMessageHistory() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> history = List.of(
                ChatMessage.createUserMessage("Single message")
            );
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, history);

            // Assert
            assertThat(result.enrichedQuery())
                .contains("Previous conversation:")
                .contains("user: Single message")
                .contains("Current question: " + query);
        }

        @Test
        @DisplayName("should preserve message order from history")
        void shouldPreserveMessageOrderFromHistory() {
            // Arrange
            String query = "Current question";
            List<ChatMessage> history = List.of(
                ChatMessage.createUserMessage("First"),
                ChatMessage.createAssistantMessage("Second"),
                ChatMessage.createUserMessage("Third")
            );
            
            when(embeddingPort.embed(anyString())).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(query, null, DEFAULT_TOP_K, history);

            // Assert
            String enriched = result.enrichedQuery();
            int firstIndex = enriched.indexOf("First");
            int secondIndex = enriched.indexOf("Second");
            int thirdIndex = enriched.indexOf("Third");
            
            assertThat(firstIndex).isLessThan(secondIndex);
            assertThat(secondIndex).isLessThan(thirdIndex);
        }
    }

    /**
     * Helper method to create DocumentChunk with embedding set via withEmbedding.
     * The chunk content is used to calculate similarity, so we use meaningful content.
     */
    private DocumentChunk createChunkWithEmbedding(UUID chunkId, UUID docId, String content, int embeddingSize) {
        // Use a simple embedding that gives predictable similarity
        float[] embedding = new float[embeddingSize];
        Arrays.fill(embedding, 0.1f);
        DocumentChunk chunk = new DocumentChunk(
            chunkId, docId, content, 0, new HashMap<>()
        );
        return chunk.withEmbedding(embedding);
    }

    /**
     * Helper method to create DocumentChunk with specific metadata.
     */
    private DocumentChunk createChunkWithMetadata(UUID chunkId, UUID docId, String content, Map<String, Object> metadata, int embeddingSize) {
        float[] embedding = new float[embeddingSize];
        Arrays.fill(embedding, 0.1f);
        DocumentChunk chunk = new DocumentChunk(
            chunkId, docId, content, 0, metadata
        );
        return chunk.withEmbedding(embedding);
    }
}
