package com.ai.rag.infrastructure;

import com.ai.rag.application.DocumentSearchService;
import com.ai.rag.domain.SourceDocument;
import com.ai.rag.domain.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("H2DocumentRetriever")
class H2DocumentRetrieverTest {

    @Mock
    private DocumentSearchService documentSearchService;

    private H2DocumentRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new H2DocumentRetriever(documentSearchService);
    }

    @Test
    @DisplayName("should_useDefaults_when_contextEmpty")
    void should_useDefaults_when_contextEmpty() {
        when(documentSearchService.retrieve(eq("what is ai"), isNull(), eq(0)))
                .thenReturn(new DocumentSearchService.RetrievalResult(
                        "ctx",
                        List.of(new SourceDocument("chunk", 0.9, Map.of()))));

        List<Document> docs = retriever.retrieve(Query.builder().text("what is ai").build());

        assertThat(docs).hasSize(1);
        assertThat(docs.getFirst().getText()).isEqualTo("chunk");
        verify(documentSearchService).retrieve("what is ai", null, 0);
    }

    @Test
    @DisplayName("should_passTopKAndDocIds_when_presentInQueryContext")
    void should_passTopKAndDocIds_when_presentInQueryContext() {
        String docId = UUID.randomUUID().toString();
        when(documentSearchService.retrieve(any(), any(), anyInt()))
                .thenReturn(new DocumentSearchService.RetrievalResult("ctx", List.of()));

        Query query = Query.builder()
                .text("filtered question")
                .context(Map.of(
                        H2DocumentRetriever.TOP_K_CONTEXT_KEY, 10,
                        H2DocumentRetriever.DOC_IDS_CONTEXT_KEY, List.of(docId)))
                .build();

        retriever.retrieve(query);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentId>> docIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentSearchService).retrieve(eq("filtered question"), docIdsCaptor.capture(), eq(10));
        assertThat(docIdsCaptor.getValue()).containsExactly(DocumentId.of(docId));
    }
}
