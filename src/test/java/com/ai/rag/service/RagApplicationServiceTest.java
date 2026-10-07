package com.ai.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.model.SourceCitation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagApplicationService")
class RagApplicationServiceTest {

  private static final String OWNER = "c:owner";

  @Mock private DocumentUploadService uploadService;
  @Mock private DocumentSearchService searchService;
  @InjectMocks private RagApplicationService service;

  @Test
  @DisplayName("should offer only ready documents when listing searchable ones")
  void shouldOfferOnlyReadyDocumentsWhenListingSearchableOnes() {
    RagDocument ready = RagDocument.startIngestion("Ready", "r.txt", 10L, OWNER);
    ready.completeIngestion(2);
    RagDocument processing = RagDocument.startIngestion("Busy", "b.txt", 10L, OWNER);
    RagDocument failed = RagDocument.startIngestion("Broken", "f.txt", 10L, OWNER);
    failed.failIngestion();
    when(uploadService.listAll(OWNER)).thenReturn(List.of(ready, processing, failed));

    assertThat(service.listSearchableDocuments(OWNER)).containsExactly(ready);
  }

  @Test
  @DisplayName("should return the retrieved context with its sources and the query")
  void shouldReturnTheRetrievedContextWithItsSourcesAndTheQuery() {
    SourceCitation source = new SourceCitation("source text", 0.95, Map.of());
    when(searchService.retrieve("test query", null, 5, OWNER))
        .thenReturn(new DocumentSearchService.RetrievalResult("context", List.of(source)));

    RagApplicationService.RetrievalResult result =
        service.retrieveContext("test query", null, 5, OWNER);

    assertThat(result.context()).isEqualTo("context");
    assertThat(result.sources()).containsExactly(source);
    assertThat(result.enrichedQuery()).isEqualTo("test query");
  }
}
