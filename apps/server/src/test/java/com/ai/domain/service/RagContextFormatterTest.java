package com.ai.domain.service;

import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RagContextFormatter Domain Service Tests
 * 
 * Tests for RagContextFormatter following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests context formatting, similarity calculation, and document title extraction
 */
@DisplayName("RagContextFormatter")
class RagContextFormatterTest {

    @Nested
    @DisplayName("calculateSimilarity")
    class CalculateSimilarity {

        @Test
        @DisplayName("should return 1.0 for identical vectors")
        void shouldReturnOneForIdenticalVectors() {
            // Arrange
            float[] vector = new float[]{0.1f, 0.2f, 0.3f, 0.4f};

            // Act
            double similarity = RagContextFormatter.calculateSimilarity(vector, vector);

            // Assert
            assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return 0.0 for all-zero vectors due to epsilon handling")
        void shouldReturnZeroForAllZeroVectorsDueToEpsilon() {
            // Arrange
            float[] zeroVector = new float[]{0.0f, 0.0f, 0.0f};

            // Act
            double similarity = RagContextFormatter.calculateSimilarity(zeroVector, zeroVector);

            // Assert - returns 0.0 due to epsilon in denominator (1e-10)
            assertThat(similarity).isZero();
        }

        @Test
        @DisplayName("should return ~0.0 for orthogonal vectors")
        void shouldReturnCloseToZeroForOrthogonalVectors() {
            // Arrange
            float[] vectorA = new float[]{1.0f, 0.0f, 0.0f};
            float[] vectorB = new float[]{0.0f, 1.0f, 0.0f};

            // Act
            double similarity = RagContextFormatter.calculateSimilarity(vectorA, vectorB);

            // Assert
            assertThat(similarity).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return ~0.0 for opposite direction vectors")
        void shouldReturnCloseToNegativeOneForOppositeVectors() {
            // Arrange
            float[] vectorA = new float[]{1.0f, 0.0f};
            float[] vectorB = new float[]{-1.0f, 0.0f};

            // Act
            double similarity = RagContextFormatter.calculateSimilarity(vectorA, vectorB);

            // Assert
            assertThat(similarity).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return 0.0 when first vector is null")
        void shouldReturnZeroWhenFirstVectorIsNull() {
            // Arrange
            float[] vector = new float[]{0.1f, 0.2f, 0.3f};

            // Act
            double similarity = RagContextFormatter.calculateSimilarity(null, vector);

            // Assert
            assertThat(similarity).isZero();
        }

        @Test
        @DisplayName("should return 0.0 when second vector is null")
        void shouldReturnZeroWhenSecondVectorIsNull() {
            // Arrange
            float[] vector = new float[]{0.1f, 0.2f, 0.3f};

            // Act
            double similarity = RagContextFormatter.calculateSimilarity(vector, null);

            // Assert
            assertThat(similarity).isZero();
        }

        @Test
        @DisplayName("should return 0.0 when both vectors are null")
        void shouldReturnZeroWhenBothVectorsAreNull() {
            // Act
            double similarity = RagContextFormatter.calculateSimilarity(null, null);

            // Assert
            assertThat(similarity).isZero();
        }

        @Test
        @DisplayName("should throw ArrayIndexOutOfBoundsException for vectors of different lengths")
        void shouldThrowArrayIndexOutOfBoundsExceptionForVectorsOfDifferentLengths() {
            // Arrange
            float[] vectorA = new float[]{1.0f, 0.0f, 0.0f, 0.0f};
            float[] vectorB = new float[]{1.0f, 0.0f};

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(
                    ArrayIndexOutOfBoundsException.class,
                    () -> RagContextFormatter.calculateSimilarity(vectorA, vectorB)
            );
        }

        @Test
        @DisplayName("should return positive value for similar vectors")
        void shouldReturnPositiveValueForSimilarVectors() {
            // Arrange
            float[] vectorA = new float[]{1.0f, 1.0f};
            float[] vectorB = new float[]{0.9f, 1.0f};

            // Act
            double similarity = RagContextFormatter.calculateSimilarity(vectorA, vectorB);

            // Assert
            assertThat(similarity).isGreaterThan(0.0);
            assertThat(similarity).isLessThan(1.0);
        }
    }

    @Nested
    @DisplayName("extractDocumentTitle")
    class ExtractDocumentTitle {

        @Test
        @DisplayName("should prefer documentTitle over filename")
        void shouldPreferDocumentTitleOverFilename() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentTitle", "Primary Title");
            metadata.put("filename", "Fallback Filename");

            // Act
            String title = RagContextFormatter.extractDocumentTitle(metadata);

            // Assert
            assertThat(title).isEqualTo("Primary Title");
        }

        @Test
        @DisplayName("should use filename when documentTitle is absent")
        void shouldUseFilenameWhenDocumentTitleIsAbsent() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", "fallback.txt");

            // Act
            String title = RagContextFormatter.extractDocumentTitle(metadata);

            // Assert
            assertThat(title).isEqualTo("fallback.txt");
        }

        @Test
        @DisplayName("should return unknown when both documentTitle and filename are absent")
        void shouldReturnUnknownWhenBothTitlesAreAbsent() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "somewhere");
            metadata.put("page", 1);

            // Act
            String title = RagContextFormatter.extractDocumentTitle(metadata);

            // Assert
            assertThat(title).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should return unknown when metadata is null")
        void shouldReturnUnknownWhenMetadataIsNull() {
            // Act
            String title = RagContextFormatter.extractDocumentTitle(null);

            // Assert
            assertThat(title).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should return unknown when metadata is empty")
        void shouldReturnUnknownWhenMetadataIsEmpty() {
            // Act
            String title = RagContextFormatter.extractDocumentTitle(Collections.emptyMap());

            // Assert
            assertThat(title).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should return documentTitle as string when present")
        void shouldReturnDocumentTitleAsStringWhenPresent() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentTitle", 12345); // Non-string value

            // Act
            String title = RagContextFormatter.extractDocumentTitle(metadata);

            // Assert
            assertThat(title).isEqualTo("12345");
        }

        @Test
        @DisplayName("should return filename as string when present")
        void shouldReturnFilenameAsStringWhenPresent() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", "document.pdf");

            // Act
            String title = RagContextFormatter.extractDocumentTitle(metadata);

            // Assert
            assertThat(title).isEqualTo("document.pdf");
        }
    }

    @Nested
    @DisplayName("extractSnippet (via formatSource)")
    class ExtractSnippet {

        @Test
        @DisplayName("should truncate content to 500 characters")
        void shouldTruncateContentTo500Characters() {
            // Arrange
            String longContent = "A".repeat(600);
            DocumentChunk chunk = createChunkWithContent(longContent);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.text()).hasSize(500);
        }

        @Test
        @DisplayName("should not truncate content at exactly 500 characters")
        void shouldNotTruncateContentAtExactly500Characters() {
            // Arrange
            String exactContent = "B".repeat(500);
            DocumentChunk chunk = createChunkWithContent(exactContent);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.text()).hasSize(500);
            assertThat(source.text()).isEqualTo(exactContent);
        }

        @Test
        @DisplayName("should not truncate content under 500 characters")
        void shouldNotTruncateContentUnder500Characters() {
            // Arrange
            String shortContent = "Short content";
            DocumentChunk chunk = createChunkWithContent(shortContent);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.text()).hasSize(shortContent.length());
            assertThat(source.text()).isEqualTo(shortContent);
        }

        @Test
        @DisplayName("should return empty text when content is null")
        void shouldReturnEmptyTextWhenContentIsNull() {
            // Arrange
            DocumentChunk chunk = createChunkWithContent(null);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.text()).isEmpty();
        }

        @Test
        @DisplayName("should return empty text when content is empty")
        void shouldReturnEmptyTextWhenContentIsEmpty() {
            // Arrange
            DocumentChunk chunk = createChunkWithContent("");

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.text()).isEmpty();
        }

        @Test
        @DisplayName("should preserve content at exact 500 character boundary")
        void shouldPreserveContentAtExact500CharacterBoundary() {
            // Arrange
            String boundaryContent = "C".repeat(499) + "X"; // Exactly 500 chars
            DocumentChunk chunk = createChunkWithContent(boundaryContent);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.text()).hasSize(500);
            assertThat(source.text()).isEqualTo(boundaryContent);
        }
    }

    @Nested
    @DisplayName("formatSourceMarker")
    class FormatSourceMarker {

        @Test
        @DisplayName("should format marker with source index")
        void shouldFormatMarkerWithSourceIndex() {
            // Arrange
            SourceDocument source = createSourceDocument(1, "text", 0.85);

            // Act
            String marker = RagContextFormatter.formatSourceMarker(source);

            // Assert
            assertThat(marker).startsWith("[Source 1]");
        }

        @Test
        @DisplayName("should include document title in marker")
        void shouldIncludeDocumentTitleInMarker() {
            // Arrange
            SourceDocument source = createSourceDocumentWithTitle(1, "text", 0.75, "My Document.pdf");

            // Act
            String marker = RagContextFormatter.formatSourceMarker(source);

            // Assert
            assertThat(marker).contains("document: My Document.pdf");
        }

        @Test
        @DisplayName("should include similarity as percentage with one decimal")
        void shouldIncludeSimilarityAsPercentageWithOneDecimal() {
            // Arrange
            SourceDocument source = createSourceDocument(1, "text", 0.756);

            // Act
            String marker = RagContextFormatter.formatSourceMarker(source);

            // Assert
            assertThat(marker).contains("similarity: 75.6%");
        }

        @Test
        @DisplayName("should format marker as complete string")
        void shouldFormatMarkerAsCompleteString() {
            // Arrange
            SourceDocument source = createSourceDocumentWithTitle(3, "content", 0.95, "Report.pdf");

            // Act
            String marker = RagContextFormatter.formatSourceMarker(source);

            // Assert
            assertThat(marker).isEqualTo("[Source 3] (document: Report.pdf, similarity: 95.0%)\n");
        }

        @Test
        @DisplayName("should handle zero similarity")
        void shouldHandleZeroSimilarity() {
            // Arrange
            SourceDocument source = createSourceDocument(5, "text", 0.0);

            // Act
            String marker = RagContextFormatter.formatSourceMarker(source);

            // Assert
            assertThat(marker).contains("similarity: 0.0%");
        }

        @Test
        @DisplayName("should handle full similarity of 1.0")
        void shouldHandleFullSimilarity() {
            // Arrange
            SourceDocument source = createSourceDocument(1, "text", 1.0);

            // Act
            String marker = RagContextFormatter.formatSourceMarker(source);

            // Assert
            assertThat(marker).contains("similarity: 100.0%");
        }
    }

    @Nested
    @DisplayName("formatSource")
    class FormatSource {

        @Test
        @DisplayName("should create source document with correct index")
        void shouldCreateSourceDocumentWithCorrectIndex() {
            // Arrange
            DocumentChunk chunk = createChunkWithContent("Sample content");

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 5, new float[]{0.1f});

            // Assert
            assertThat(source.index()).isEqualTo(5);
        }

        @Test
        @DisplayName("should calculate and store similarity score")
        void shouldCalculateAndStoreSimilarityScore() {
            // Arrange
            DocumentChunk chunk = createChunkWithContent("Content");
            float[] queryEmbedding = new float[]{1.0f, 0.0f};

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, queryEmbedding);

            // Assert
            assertThat(source.score()).isGreaterThanOrEqualTo(0.0);
            assertThat(source.score()).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("should extract document title from metadata")
        void shouldExtractDocumentTitleFromMetadata() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentTitle", "AI Guidelines.pdf");
            DocumentChunk chunk = createChunkWithMetadata("Content", metadata);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.documentTitle()).isEqualTo("AI Guidelines.pdf");
        }

        @Test
        @DisplayName("should include original metadata in source document")
        void shouldIncludeOriginalMetadataInSourceDocument() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentTitle", "Test");
            metadata.put("page", 42);
            DocumentChunk chunk = createChunkWithMetadata("Content", metadata);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.metadata()).containsEntry("page", 42);
        }

        @Test
        @DisplayName("should handle null metadata in source document")
        void shouldHandleNullMetadataInSourceDocument() {
            // Arrange
            DocumentChunk chunk = createChunkWithMetadata("Content", null);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.metadata()).isNull();
            assertThat(source.documentTitle()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should use unknown for missing document title")
        void shouldUseUnknownForMissingDocumentTitle() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("otherKey", "value");
            DocumentChunk chunk = createChunkWithMetadata("Content", metadata);

            // Act
            SourceDocument source = RagContextFormatter.formatSource(chunk, 1, new float[]{0.1f});

            // Assert
            assertThat(source.documentTitle()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("buildContextWithSources")
    class BuildContextWithSources {

        @Test
        @DisplayName("should build context from single chunk")
        void shouldBuildContextFromSingleChunk() {
            // Arrange
            List<DocumentChunk> chunks = List.of(createChunkWithContent("First chunk content"));

            // Act
            String context = RagContextFormatter.buildContextWithSources(chunks, new float[]{0.1f});

            // Assert
            assertThat(context).contains("[Source 1]");
            assertThat(context).contains("First chunk content");
        }

        @Test
        @DisplayName("should separate chunks with double newline")
        void shouldSeparateChunksWithDoubleNewline() {
            // Arrange
            List<DocumentChunk> chunks = List.of(
                    createChunkWithContent("First"),
                    createChunkWithContent("Second"),
                    createChunkWithContent("Third")
            );

            // Act
            String context = RagContextFormatter.buildContextWithSources(chunks, new float[]{0.1f});

            // Assert
            assertThat(context).contains("First\n\n");
            assertThat(context).contains("Second\n\n");
            assertThat(context).contains("Third\n\n");
        }

        @Test
        @DisplayName("should assign sequential source indices starting at 1")
        void shouldAssignSequentialSourceIndicesStartingAtOne() {
            // Arrange
            List<DocumentChunk> chunks = List.of(
                    createChunkWithContent("First"),
                    createChunkWithContent("Second"),
                    createChunkWithContent("Third")
            );

            // Act
            String context = RagContextFormatter.buildContextWithSources(chunks, new float[]{0.1f});

            // Assert
            assertThat(context).contains("[Source 1]");
            assertThat(context).contains("[Source 2]");
            assertThat(context).contains("[Source 3]");
        }

        @Test
        @DisplayName("should handle empty chunk list")
        void shouldHandleEmptyChunkList() {
            // Arrange
            List<DocumentChunk> chunks = Collections.emptyList();

            // Act
            String context = RagContextFormatter.buildContextWithSources(chunks, new float[]{0.1f});

            // Assert
            assertThat(context).isEmpty();
        }

        @Test
        @DisplayName("should build context with correct source marker format")
        void shouldBuildContextWithCorrectSourceMarkerFormat() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentTitle", "Guide.pdf");
            DocumentChunk chunk = createChunkWithMetadata("Content here", metadata);

            // Act
            String context = RagContextFormatter.buildContextWithSources(List.of(chunk), new float[]{0.5f});

            // Assert
            assertThat(context).startsWith("[Source 1] (document: Guide.pdf, similarity:");
            assertThat(context).endsWith(")\nContent here\n\n");
        }

        @Test
        @DisplayName("should preserve content order in context")
        void shouldPreserveContentOrderInContext() {
            // Arrange
            List<DocumentChunk> chunks = List.of(
                    createChunkWithContent("Alpha"),
                    createChunkWithContent("Beta"),
                    createChunkWithContent("Gamma")
            );

            // Act
            String context = RagContextFormatter.buildContextWithSources(chunks, new float[]{0.1f});

            // Assert
            int alphaIndex = context.indexOf("Alpha");
            int betaIndex = context.indexOf("Beta");
            int gammaIndex = context.indexOf("Gamma");

            assertThat(alphaIndex).isLessThan(betaIndex);
            assertThat(betaIndex).isLessThan(gammaIndex);
        }

        @Test
        @DisplayName("should handle chunks with different metadata")
        void shouldHandleChunksWithDifferentMetadata() {
            // Arrange
            Map<String, Object> metadata1 = new HashMap<>();
            metadata1.put("documentTitle", "Document A");
            Map<String, Object> metadata2 = new HashMap<>();
            metadata2.put("documentTitle", "Document B");

            List<DocumentChunk> chunks = List.of(
                    createChunkWithMetadata("Content A", metadata1),
                    createChunkWithMetadata("Content B", metadata2)
            );

            // Act
            String context = RagContextFormatter.buildContextWithSources(chunks, new float[]{0.1f});

            // Assert
            assertThat(context).contains("Document A");
            assertThat(context).contains("Document B");
        }

        @Test
        @DisplayName("should return trailing newline after last chunk")
        void shouldReturnTrailingNewlineAfterLastChunk() {
            // Arrange
            DocumentChunk chunk = createChunkWithContent("Single content");

            // Act
            String context = RagContextFormatter.buildContextWithSources(List.of(chunk), new float[]{0.1f});

            // Assert
            assertThat(context).endsWith("\n\n");
        }
    }

    // ========== Helper Methods ==========

    private DocumentChunk createChunkWithContent(String content) {
        return createChunkWithMetadata(content, new HashMap<>());
    }

    private DocumentChunk createChunkWithMetadata(String content, Map<String, Object> metadata) {
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        DocumentChunk chunk = new DocumentChunk(chunkId, docId, content, 0, metadata);
        return chunk.withEmbedding(new float[]{0.1f, 0.1f});
    }

    private SourceDocument createSourceDocument(int index, String text, double score) {
        return createSourceDocumentWithTitle(index, text, score, "unknown");
    }

    private SourceDocument createSourceDocumentWithTitle(int index, String text, double score, String title) {
        return new SourceDocument(index, text, score, title, new HashMap<>());
    }
}
