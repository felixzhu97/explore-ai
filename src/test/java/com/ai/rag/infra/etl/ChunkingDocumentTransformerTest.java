package com.ai.rag.infra.etl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.rag.domain.model.ExtractedDocument;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

@DisplayName("ChunkingDocumentTransformer")
class ChunkingDocumentTransformerTest {

  private ChunkingDocumentTransformer transformer;

  @BeforeEach
  void setUp() {
    TokenTextSplitter splitter =
        TokenTextSplitter.builder()
            .withChunkSize(20)
            .withMinChunkSizeChars(1)
            .withMinChunkLengthToEmbed(1)
            .build();
    transformer = new ChunkingDocumentTransformer(splitter);
  }

  @Test
  @DisplayName("should create raw documents for chunks preserving metadata and source")
  void shouldCreateExtractedDocumentsForChunksPreservingMetadataAndSource() {
    Map<String, Object> metadata = Map.of("fileName", "guide.txt", "category", "docs");
    String content =
        "First paragraph with enough words to exceed the token limit. "
            + "Second paragraph also needs sufficient length for another chunk boundary.";
    ExtractedDocument document = new ExtractedDocument(content, metadata, "guide.txt");

    List<ExtractedDocument> chunks = transformer.splitDocument(document);

    assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
    assertThat(chunks)
        .allSatisfy(
            chunk -> {
              assertThat(chunk.metadata()).isEqualTo(metadata);
              assertThat(chunk.source()).isEqualTo("guide.txt");
              assertThat(chunk.content()).isNotBlank();
            });
  }

  @Test
  @DisplayName("should return empty list when content is blank")
  void shouldReturnEmptyListWhenContentIsBlank() {
    ExtractedDocument document =
        new ExtractedDocument("   ", Map.of("fileName", "blank.txt"), "blank.txt");

    List<ExtractedDocument> chunks = transformer.splitDocument(document);

    assertThat(chunks).isEmpty();
  }

  @Test
  @DisplayName("should return single chunk when text fits token limit")
  void shouldReturnSingleChunkWhenTextFitsTokenLimit() {
    ExtractedDocument document =
        new ExtractedDocument("Short note.", Map.of("fileName", "short.txt"), "short.txt");

    List<ExtractedDocument> chunks = transformer.splitDocument(document);

    assertThat(chunks).hasSize(1);
    assertThat(chunks.getFirst().content()).isEqualTo("Short note.");
  }
}
