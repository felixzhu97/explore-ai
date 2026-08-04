package com.ai.rag.infrastructure;

import com.ai.rag.domain.RawDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChunkingDocumentTransformer")
class ChunkingDocumentTransformerTest {

    private ChunkingDocumentTransformer transformer;

    @BeforeEach
    void setUp() {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(20)
                .withMinChunkSizeChars(1)
                .withMinChunkLengthToEmbed(1)
                .build();
        transformer = new ChunkingDocumentTransformer(splitter);
    }

    @Test
    @DisplayName("should create raw documents for chunks preserving metadata and source")
    void should_create_raw_documents_for_chunks_preserving_metadata_and_source() {
        Map<String, Object> metadata = Map.of("fileName", "guide.txt", "category", "docs");
        String content = "First paragraph with enough words to exceed the token limit. "
                + "Second paragraph also needs sufficient length for another chunk boundary.";
        RawDocument document = new RawDocument(content, metadata, "guide.txt");

        List<RawDocument> chunks = transformer.transform(document);

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks)
                .allSatisfy(chunk -> {
                    assertThat(chunk.metadata()).isEqualTo(metadata);
                    assertThat(chunk.source()).isEqualTo("guide.txt");
                    assertThat(chunk.content()).isNotBlank();
                });
    }

    @Test
    @DisplayName("should return empty list when content is blank")
    void should_return_empty_list_when_content_is_blank() {
        RawDocument document = new RawDocument("   ", Map.of("fileName", "blank.txt"), "blank.txt");

        List<RawDocument> chunks = transformer.transform(document);

        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("should return single chunk when text fits token limit")
    void should_return_single_chunk_when_text_fits_token_limit() {
        RawDocument document = new RawDocument("Short note.", Map.of("fileName", "short.txt"), "short.txt");

        List<RawDocument> chunks = transformer.transform(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().content()).isEqualTo("Short note.");
    }
}
