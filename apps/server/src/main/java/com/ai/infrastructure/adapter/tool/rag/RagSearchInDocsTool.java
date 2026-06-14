package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.DocumentChunk;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import com.ai.infrastructure.adapter.tool.JsonSchemaBuilder;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool for searching within specific documents.
 */
@Component
public class RagSearchInDocsTool implements ToolProvider {

    private final EmbeddingPort embeddingPort;
    private final VectorSearchPort vectorSearchPort;

    public RagSearchInDocsTool(EmbeddingPort embeddingPort, VectorSearchPort vectorSearchPort) {
        this.embeddingPort = embeddingPort;
        this.vectorSearchPort = vectorSearchPort;
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.stringProp("query", "The search query text", true),
            JsonSchemaBuilder.arrayProp("documentIds", "List of document IDs to search within", "string", true),
            JsonSchemaBuilder.integerProp("topK", "Maximum number of results (default 5)", false)
        );
        return ToolDefinition.atomic(
            "rag_search_in_docs",
            "Search for relevant document chunks within specific documents by their IDs.",
            JsonSchemaBuilder.objectSchema(List.of("query", "documentIds"), props),
            "rag"
        );
    }

    @Override
    public ToolExecutor executor() {
        return invocation -> execute(invocation);
    }

    @SuppressWarnings("unchecked")
    public ToolResult execute(ToolInvocation invocation) {
        String query = invocation.getArg("query", "");
        List<String> docIdStrings = invocation.getArg("documentIds", List.of());
        int topK = invocation.getArg("topK", 5);

        if (query == null || query.isBlank()) {
            return ToolResult.error("Query cannot be empty");
        }
        if (docIdStrings == null || docIdStrings.isEmpty()) {
            return ToolResult.error("documentIds cannot be empty");
        }

        try {
            List<UUID> docIds = new ArrayList<>();
            for (String id : docIdStrings) {
                docIds.add(UUID.fromString(id.trim()));
            }

            float[] queryEmbedding = embeddingPort.embed(query);
            var chunks = vectorSearchPort.search(queryEmbedding, topK, docIds);

            List<Map<String, Object>> results = new ArrayList<>();
            StringBuilder md = new StringBuilder();

            for (DocumentChunk chunk : chunks) {
                double score = cosineSimilarity(queryEmbedding, chunk.getEmbedding());

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("docId", chunk.getMetadata() != null ? chunk.getMetadata().get("documentId") : null);
                item.put("title", chunk.getMetadata() != null ? chunk.getMetadata().get("title") : "Unknown");
                item.put("snippet", chunk.getContent().length() > 300
                    ? chunk.getContent().substring(0, 300) + "..."
                    : chunk.getContent());
                item.put("score", Math.round(score * 1000.0) / 1000.0);

                results.add(item);
                md.append("- **").append(item.get("title")).append("** (score: ")
                  .append(String.format("%.3f", score)).append(")\n")
                  .append("  `").append(item.get("docId")).append("`\n")
                  .append("  > ").append(item.get("snippet")).append("\n\n");
            }

            if (results.isEmpty()) {
                return ToolResult.success("No relevant documents found in specified documents for query: " + query);
            }

            Map<String, Object> structured = Map.of(
                "query", query,
                "searchedDocIds", docIdStrings,
                "total", results.size(),
                "results", results
            );

            return ToolResult.success(md.toString(), structured);
        } catch (IllegalArgumentException e) {
            return ToolResult.error("Invalid document ID format: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("RAG search in docs failed: " + e.getMessage());
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }
}
