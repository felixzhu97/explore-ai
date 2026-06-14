package com.ai.infrastructure.adapter.ai;

import com.ai.application.service.RagApplicationService;
import com.ai.domain.model.SourceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Tool for searching documents in the RAG system.
 * Provides AI models with the ability to search for relevant documents.
 *
 * @deprecated Use {@code rag_search} / {@code rag_search_in_docs} (tool platform) instead.
 */
@java.lang.Deprecated(since = "0.2.0", forRemoval = true)
@Component
public class DocumentSearchTool {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchTool.class);

    private final RagApplicationService ragApplicationService;

    public DocumentSearchTool(RagApplicationService ragApplicationService) {
        this.ragApplicationService = ragApplicationService;
    }

    /**
     * Search for documents related to a query.
     *
     * @param query the search query
     * @param maxResults maximum number of results to return (default 5)
     * @return search results as a formatted string
     */
    @Tool(description = "Search for relevant documents based on a query. Returns document chunks with relevance scores.")
    public String searchDocuments(
            @ToolParam(description = "The search query to find relevant documents", required = true) String query,
            @ToolParam(description = "Maximum number of results to return (default 5)") Integer maxResults) {
        log.info("Tool search request: {}, maxResults: {}", query, maxResults);

        try {
            int topK = maxResults != null ? maxResults : 5;
            var result = ragApplicationService.retrieveContext(query, null, topK);

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(result.sources().size()).append(" relevant documents:\n\n");

            for (int i = 0; i < result.sources().size(); i++) {
                SourceDocument doc = result.sources().get(i);
                sb.append("Document ").append(i + 1).append(":\n");
                sb.append("Content: ").append(doc.text()).append("\n");
                sb.append("Relevance: ").append(String.format("%.2f", doc.score())).append("\n\n");
            }

            log.info("Tool search returned {} results", result.sources().size());
            return sb.toString();
        } catch (Exception e) {
            log.error("Error in document search tool", e);
            return "Error searching documents: " + e.getMessage();
        }
    }

    /**
     * Search for documents within specific documents by IDs.
     *
     * @param query the search query
     * @param docIds comma-separated list of document IDs to search within
     * @param maxResults maximum number of results to return (default 5)
     * @return search results as a formatted string
     */
    @Tool(description = "Search for relevant documents within specific documents by their IDs.")
    public String searchDocumentsInIds(
            @ToolParam(description = "The search query to find relevant documents", required = true) String query,
            @ToolParam(description = "Comma-separated list of document IDs to search within") String docIds,
            @ToolParam(description = "Maximum number of results to return (default 5)") Integer maxResults) {
        log.info("Tool search with IDs request: {}, docIds: {}", query, docIds);

        try {
            List<String> idList = List.of(docIds.split(","));
            List<UUID> uuidList = idList.stream()
                    .map(String::trim)
                    .map(UUID::fromString)
                    .toList();

            int topK = maxResults != null ? maxResults : 5;
            var result = ragApplicationService.retrieveContext(query, uuidList, topK);

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(result.sources().size()).append(" relevant documents:\n\n");

            for (int i = 0; i < result.sources().size(); i++) {
                SourceDocument doc = result.sources().get(i);
                sb.append("Document ").append(i + 1).append(":\n");
                sb.append("Content: ").append(doc.text()).append("\n");
                sb.append("Relevance: ").append(String.format("%.2f", doc.score())).append("\n\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error in document search tool with IDs", e);
            return "Error searching documents: " + e.getMessage();
        }
    }
}
