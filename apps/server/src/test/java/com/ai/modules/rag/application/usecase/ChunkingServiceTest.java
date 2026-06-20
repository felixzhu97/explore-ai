package com.ai.modules.rag.application.usecase;

import com.ai.modules.rag.application.usecase.ChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChunkingService")
class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService(100, 20);
    }

    @Nested
    @DisplayName("chunk()")
    class ChunkMethod {

        @Test
        @DisplayName("should return empty list for null input")
        void shouldReturnEmptyListForNullInput() {
            List<String> chunks = chunkingService.chunk(null);
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank input")
        void shouldReturnEmptyListForBlankInput() {
            List<String> chunks = chunkingService.chunk("   ");
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("should return single chunk for text smaller than chunk size")
        void shouldReturnSingleChunkForSmallText() {
            String text = "This is a short text.";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEqualTo(text);
        }

        @Test
        @DisplayName("should split text by paragraphs")
        void shouldSplitTextByParagraphs() {
            String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).hasSize(3);
            assertThat(chunks).contains("First paragraph.", "Second paragraph.", "Third paragraph.");
        }

        @Test
        @DisplayName("should handle long paragraphs with recursive splitting")
        void shouldHandleLongParagraphs() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append("This is sentence number ").append(i).append(". ");
            }
            String text = sb.toString();

            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isNotEmpty();
            for (String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(150);
            }
        }

        @Test
        @DisplayName("should preserve sentence boundaries when possible")
        void shouldPreserveSentenceBoundaries() {
            String text = "First sentence. Second sentence. Third sentence. Fourth sentence.";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isNotEmpty();
            for (String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(110);
            }
        }

        @Test
        @DisplayName("should handle Chinese text")
        void shouldHandleChineseText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                sb.append("这是一段中文文本。");
            }
            String text = sb.toString();

            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should filter out empty chunks")
        void shouldFilterEmptyChunks() {
            String text = "   \n\n\n   ";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("should handle single character text")
        void shouldHandleSingleCharacterText() {
            String text = "A";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEqualTo("A");
        }

        @Test
        @DisplayName("should handle text with only spaces")
        void shouldHandleTextWithOnlySpaces() {
            String text = "     ";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("should handle text without any spaces")
        void shouldHandleTextWithoutAnySpaces() {
            String text = "ThisIsAVeryLongTextWithoutSpaces1234567890";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isNotEmpty();
            for (String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(110);
            }
        }

        @ParameterizedTest
        @CsvSource({
            "'Hello World', 1",
            "'Short text', 1",
            "'Multiple\n\nparagraphs\n\nhere', 3"
        })
        @DisplayName("should chunk text with various sizes")
        void shouldChunkTextWithVariousSizes(String text, int expectedMinChunks) {
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).hasSizeGreaterThanOrEqualTo(expectedMinChunks);
        }
    }

    @Nested
    @DisplayName("chunk() with custom separators")
    class ChunkWithSeparatorsMethod {

        @Test
        @DisplayName("should use custom separators")
        void shouldUseCustomSeparators() {
            String text = "Part1---Part2---Part3";
            String[] separators = {"---", "-"};

            List<String> chunks = chunkingService.chunk(text, separators);
            assertThat(chunks).contains("Part1", "Part2", "Part3");
        }

        @Test
        @DisplayName("should handle null separator in array")
        void shouldHandleNullSeparatorInArray() {
            String text = "First\n\nSecond";
            String[] separators = {null, "\n"};
            List<String> chunks = chunkingService.chunk(text, separators);
            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle empty string separator")
        void shouldHandleEmptyStringSeparator() {
            String text = "First\n\nSecond";
            String[] separators = {""};
            List<String> chunks = chunkingService.chunk(text, separators);
            assertThat(chunks).hasSize(1);
        }

        @Test
        @DisplayName("should handle single space separator")
        void shouldHandleSingleSpaceSeparator() {
            String text = "First Second Third";
            String[] separators = {" "};
            List<String> chunks = chunkingService.chunk(text, separators);
            assertThat(chunks).hasSize(1);
        }

        @Test
        @DisplayName("should recurse through separators for long text")
        void shouldRecurseThroughSeparatorsForLongText() {
            // Test the recursiveChunkWithSeparators method by using multiple separator levels
            String longText = "Long ".repeat(30); // Very long text
            String[] separators = {"---", ", ", " "};

            List<String> chunks = chunkingService.chunk(longText, separators);

            // Should split into multiple chunks
            assertThat(chunks).isNotEmpty();
            for (String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(110);
            }
        }

        @Test
        @DisplayName("should handle text longer than chunk size with custom separators")
        void shouldHandleTextLongerThanChunkSizeWithCustomSeparators() {
            String longText = "A".repeat(150) + "---" + "B".repeat(150);
            String[] separators = {"---"};

            List<String> chunks = chunkingService.chunk(longText, separators);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle custom separators with mixed lengths")
        void shouldHandleCustomSeparatorsWithMixedLengths() {
            String text = "Part1||Part2||Part3";
            String[] separators = {"||", "|", ","};

            List<String> chunks = chunkingService.chunk(text, separators);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should filter empty chunks with custom separators")
        void shouldFilterEmptyChunksWithCustomSeparators() {
            String text = "   \n\n\n   ";
            String[] separators = {"\n"};
            List<String> chunks = chunkingService.chunk(text, separators);
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("should handle short separators array")
        void shouldHandleShortSeparatorsArray() {
            String text = "FirstSecondThird";
            String[] separators = {"X"};

            List<String> chunks = chunkingService.chunk(text, separators);

            // No "X" found, should return single chunk
            assertThat(chunks).hasSize(1);
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should use default chunk size and overlap")
        void shouldUseDefaultValues() {
            ChunkingService service = new ChunkingService(500, 50);
            String text = "A".repeat(1000);
            List<String> chunks = service.chunk(text);

            for (String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(550);
            }
        }

        @Test
        @DisplayName("should handle zero overlap")
        void shouldHandleZeroOverlap() {
            ChunkingService service = new ChunkingService(100, 0);
            String text = "A".repeat(300);
            List<String> chunks = service.chunk(text);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle large chunk size")
        void shouldHandleLargeChunkSize() {
            ChunkingService service = new ChunkingService(1000, 0);
            String text = "A".repeat(500);
            List<String> chunks = service.chunk(text);

            assertThat(chunks).hasSize(1);
        }
    }

    @Nested
    @DisplayName("recursive splitting")
    class RecursiveSplitting {

        @Test
        @DisplayName("should recursively split very long text")
        void shouldRecursivelySplitVeryLongText() {
            String text = "Word ".repeat(500);
            List<String> chunks = chunkingService.chunk(text);

            assertThat(chunks).isNotEmpty();
            for (String chunk : chunks) {
                assertThat(chunk.length()).isLessThanOrEqualTo(110);
            }
        }

        @Test
        @DisplayName("should handle text requiring multiple recursion levels")
        void shouldHandleTextRequiringMultipleRecursionLevels() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                sb.append("LongWord").append(i).append(" ");
            }
            String text = sb.toString();

            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle text exactly at chunk size boundary")
        void shouldHandleTextExactlyAtChunkSizeBoundary() {
            // Use a service with chunk size of 100
            ChunkingService service100 = new ChunkingService(100, 0);
            String text = "A".repeat(100);

            List<String> chunks = service100.chunk(text);
            assertThat(chunks).hasSize(1);
        }

        @Test
        @DisplayName("should handle text one character over chunk size")
        void shouldHandleTextOneCharOverChunkSize() {
            ChunkingService service100 = new ChunkingService(100, 0);
            String text = "A".repeat(101);

            List<String> chunks = service100.chunk(text);
            assertThat(chunks).hasSizeGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("splitBySeparator")
    class SplitBySeparator {

        @Test
        @DisplayName("should handle separator at boundaries")
        void shouldHandleSeparatorAtBoundaries() {
            String text = "\n\nText\n\n";
            List<String> chunks = chunkingService.chunk(text);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle consecutive separators")
        void shouldHandleConsecutiveSeparators() {
            String text = "Word1,,,Word2";
            String[] separators = {","};
            List<String> chunks = chunkingService.chunk(text, separators);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle edge case in splitBySize - chunk at end of text")
        void shouldHandleChunkAtEndOfText() {
            // Test the else branch in splitBySize when end >= text.length()
            ChunkingService service = new ChunkingService(50, 0);
            String text = "Short";

            List<String> chunks = service.chunk(text);
            assertThat(chunks).hasSize(1);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle newline characters without paragraph breaks")
        void shouldHandleNewlineCharactersWithoutParagraphBreaks() {
            String text = "Line1\nLine2\nLine3";
            List<String> chunks = chunkingService.chunk(text);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle various punctuation marks")
        void shouldHandleVariousPunctuationMarks() {
            String text = "Question? Answer! Statement. Comma, here; and there.";
            List<String> chunks = chunkingService.chunk(text);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle unicode text")
        void shouldHandleUnicodeText() {
            String text = "日本語のテキストと English mixed together";
            List<String> chunks = chunkingService.chunk(text);

            assertThat(chunks).isNotEmpty();
        }

        @Test
        @DisplayName("should handle emoji characters")
        void shouldHandleEmojiCharacters() {
            String text = "Hello! 🎉🎊🎈 Celebration time! 🎁🎀🎄";
            List<String> chunks = chunkingService.chunk(text);

            assertThat(chunks).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "Simple text without any special chars",
            "Text with\ttab\tcharacters",
            "Text with\r\nCRLF\r\nline breaks",
            "Text with\rsingle\rcarriage\returns"
        })
        @DisplayName("should handle various whitespace characters")
        void shouldHandleVariousWhitespaceCharacters(String text) {
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).isNotEmpty();
        }
    }
}
