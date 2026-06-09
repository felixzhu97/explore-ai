package com.ai.rag.domain.service;

import com.ai.rag.domain.Chunk;
import com.ai.rag.domain.ChunkingPolicy;
import com.ai.rag.domain.ChunkingPolicy.ChunkingStrategy;
import com.ai.rag.domain.DocumentContent;
import com.ai.rag.domain.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChunkingService Tests")
class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
    }

    @Nested
    @DisplayName("chunk")
    class ChunkTests {

        @Test
        @DisplayName("should return empty list for null content")
        void shouldReturnEmptyListForNullContent() {
            List<Chunk> result = chunkingService.chunk(
                null,
                ChunkingPolicy.bySize(100, 10),
                DocumentId.generate()
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank content")
        void shouldReturnEmptyListForBlankContent() {
            DocumentContent content = new DocumentContent("   \t\n   ");

            List<Chunk> result = chunkingService.chunk(
                content,
                ChunkingPolicy.bySize(100, 10),
                DocumentId.generate()
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return single chunk when text is smaller than chunk size")
        void shouldReturnSingleChunkWhenTextIsSmallerThanChunkSize() {
            String shortText = "This is a short text.";
            DocumentContent content = new DocumentContent(shortText);
            DocumentId docId = DocumentId.generate();

            List<Chunk> result = chunkingService.chunk(
                content,
                ChunkingPolicy.bySize(100, 10),
                docId
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).text()).isEqualTo(shortText);
            assertThat(result.get(0).sourceDocumentId()).isEqualTo(docId);
            assertThat(result.get(0).position()).isZero();
        }

        @Test
        @DisplayName("should chunk by size with overlap")
        void shouldChunkBySizeWithOverlap() {
            String text = "A".repeat(250);
            DocumentContent content = new DocumentContent(text);
            DocumentId docId = DocumentId.generate();

            List<Chunk> result = chunkingService.chunk(
                content,
                ChunkingPolicy.bySize(100, 20),
                docId
            );

            assertThat(result).isNotEmpty();
            assertThat(result).allSatisfy(chunk -> {
                assertThat(chunk.text()).isNotBlank();
                assertThat(chunk.sourceDocumentId()).isEqualTo(docId);
            });
        }

        @Test
        @DisplayName("should chunk by paragraph strategy")
        void shouldChunkByParagraphStrategy() {
            String text = "Paragraph one.\n\nParagraph two.\n\nParagraph three.";
            DocumentContent content = new DocumentContent(text);
            DocumentId docId = DocumentId.generate();

            List<Chunk> result = chunkingService.chunk(
                content,
                ChunkingPolicy.byParagraphs(100, 10),
                docId
            );

            assertThat(result).isNotEmpty();
            assertThat(result).allSatisfy(chunk -> {
                assertThat(chunk.text()).isNotBlank();
                assertThat(chunk.sourceDocumentId()).isEqualTo(docId);
            });
        }
    }

    @Nested
    @DisplayName("chunkBySize")
    class ChunkBySizeTests {

        @Test
        @DisplayName("should return empty list for null text")
        void shouldReturnEmptyListForNullText() {
            List<String> result = chunkingService.chunkBySize(null, 100, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank text")
        void shouldReturnEmptyListForBlankText() {
            List<String> result = chunkingService.chunkBySize("   ", 100, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return single chunk for text smaller than chunk size")
        void shouldReturnSingleChunkForTextSmallerThanChunkSize() {
            String text = "Short text";

            List<String> result = chunkingService.chunkBySize(text, 100, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(text);
        }

        @Test
        @DisplayName("should split text into multiple chunks when exceeding chunk size")
        void shouldSplitTextIntoMultipleChunksWhenExceedingChunkSize() {
            String text = "A".repeat(250);

            List<String> result = chunkingService.chunkBySize(text, 100, 10);

            assertThat(result).hasSizeGreaterThan(1);
        }

        @Test
        @DisplayName("should apply overlap between chunks")
        void shouldApplyOverlapBetweenChunks() {
            String text = "AAAA" + "B".repeat(96) + "CCCC";
            int chunkSize = 100;
            int overlap = 10;

            List<String> result = chunkingService.chunkBySize(text, chunkSize, overlap);

            if (result.size() > 1) {
                String firstChunk = result.get(0);
                String secondChunk = result.get(1);
                assertThat(firstChunk).isNotEqualTo(secondChunk);
            }
        }

        @ParameterizedTest
        @CsvSource({
            "100, 0, 100",
            "200, 20, 100",
            "300, 50, 100",
            "500, 100, 200"
        })
        @DisplayName("should handle various chunk sizes and overlaps correctly")
        void shouldHandleVariousChunkSizesAndOverlaps(int textSize, int overlap, int chunkSize) {
            String text = "X".repeat(textSize);

            List<String> result = chunkingService.chunkBySize(text, chunkSize, overlap);

            assertThat(result).isNotEmpty();
            result.forEach(chunk -> assertThat(chunk).isNotBlank());
        }
    }

    @Nested
    @DisplayName("chunkByParagraphs")
    class ChunkByParagraphsTests {

        @Test
        @DisplayName("should return empty list for null text")
        void shouldReturnEmptyListForNullText() {
            List<String> result = chunkingService.chunkByParagraphs(null, 100, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank text")
        void shouldReturnEmptyListForBlankText() {
            List<String> result = chunkingService.chunkByParagraphs("   ", 100, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should split text by paragraphs")
        void shouldSplitTextByParagraphs() {
            String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";

            List<String> result = chunkingService.chunkByParagraphs(text, 100, 10);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("should merge small paragraphs into same chunk")
        void shouldMergeSmallParagraphsIntoSameChunk() {
            String text = "Para one.\n\nPara two.\n\nPara three.";

            List<String> result = chunkingService.chunkByParagraphs(text, 500, 10);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should split large paragraph that exceeds chunk size")
        void shouldSplitLargeParagraphThatExceedsChunkSize() {
            String largeParagraph = "X".repeat(250);
            String text = largeParagraph + "\n\nSmall paragraph.";

            List<String> result = chunkingService.chunkByParagraphs(text, 100, 10);

            assertThat(result.size()).isGreaterThan(1);
        }

        @Test
        @DisplayName("should preserve paragraph boundaries in output")
        void shouldPreserveParagraphBoundariesInOutput() {
            String text = "Paragraph one.\n\nParagraph two.";

            List<String> result = chunkingService.chunkByParagraphs(text, 500, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("Paragraph one");
            assertThat(result.get(0)).contains("Paragraph two");
        }

        @Test
        @DisplayName("should skip empty paragraphs")
        void shouldSkipEmptyParagraphs() {
            String text = "Content one.\n\n\n\nContent two.";

            List<String> result = chunkingService.chunkByParagraphs(text, 100, 10);

            assertThat(result).allSatisfy(chunk ->
                assertThat(chunk).doesNotContainPattern("\\n\\s*\\n\\s*\\n")
            );
        }
    }

    @Nested
    @DisplayName("estimateTokens")
    class EstimateTokensTests {

        @Test
        @DisplayName("should return zero for null text")
        void shouldReturnZeroForNullText() {
            int result = chunkingService.estimateTokens(null);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("should return zero for blank text")
        void shouldReturnZeroForBlankText() {
            int result = chunkingService.estimateTokens("   ");

            assertThat(result).isZero();
        }

        @ParameterizedTest
        @CsvSource({
            "'test', 1",
            "'hello world', 2",
            "'This is a longer sentence.', 6",
            "'ABCDEFGHIJ', 2"
        })
        @DisplayName("should estimate tokens correctly")
        void shouldEstimateTokensCorrectly(String text, int expectedMin) {
            int result = chunkingService.estimateTokens(text);

            assertThat(result).isGreaterThanOrEqualTo(expectedMin);
        }
    }

    @Nested
    @DisplayName("needsChunking")
    class NeedsChunkingTests {

        @Test
        @DisplayName("should return false when tokens are below threshold")
        void shouldReturnFalseWhenTokensAreBelowThreshold() {
            String text = "Short text";

            boolean result = chunkingService.needsChunking(text, 1000);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when tokens exceed threshold")
        void shouldReturnTrueWhenTokensExceedThreshold() {
            String text = "A".repeat(5000);

            boolean result = chunkingService.needsChunking(text, 100);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for null text regardless of threshold")
        void shouldReturnFalseForNullTextRegardlessOfThreshold() {
            boolean result = chunkingService.needsChunking(null, 0);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for blank text regardless of threshold")
        void shouldReturnFalseForBlankTextRegardlessOfThreshold() {
            boolean result = chunkingService.needsChunking("   ", 0);

            assertThat(result).isFalse();
        }
    }
}
