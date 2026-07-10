package com.ai.rag.infrastructure.etl;

import com.ai.rag.application.usecase.ChunkingService;
import com.ai.rag.domain.model.RawDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChunkingDocumentTransformer")
class ChunkingDocumentTransformerTest {

    @Mock
    private ChunkingService chunkingService;

    private ChunkingDocumentTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new ChunkingDocumentTransformer(chunkingService);
    }

    @Test
    @DisplayName("should create raw documents for chunks preserving metadata and source")
    void should_create_raw_documents_for_chunks_preserving_metadata_and_source() {
        Map<String, Object> metadata = Map.of("fileName", "guide.txt", "category", "docs");
        RawDocument document = new RawDocument("first paragraph second paragraph", metadata, "guide.txt");
        when(chunkingService.chunk(document.content())).thenReturn(List.of("first paragraph", "second paragraph"));

        List<RawDocument> chunks = transformer.transform(document);

        assertThat(chunks)
                .hasSize(2)
                .extracting(RawDocument::content)
                .containsExactly("first paragraph", "second paragraph");
        assertThat(chunks)
                .allSatisfy(chunk -> {
                    assertThat(chunk.metadata()).isEqualTo(metadata);
                    assertThat(chunk.source()).isEqualTo("guide.txt");
                });
        verify(chunkingService).chunk(document.content());
    }

    @Test
    @DisplayName("should return empty list when chunking produces no content")
    void should_return_empty_list_when_chunking_produces_no_content() {
        RawDocument document = new RawDocument("   ", Map.of("fileName", "blank.txt"), "blank.txt");
        when(chunkingService.chunk(document.content())).thenReturn(List.of());

        List<RawDocument> chunks = transformer.transform(document);

        assertThat(chunks).isEmpty();
        verify(chunkingService).chunk(document.content());
    }
}
