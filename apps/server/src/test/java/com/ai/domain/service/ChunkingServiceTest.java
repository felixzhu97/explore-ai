package com.ai.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    }
}
