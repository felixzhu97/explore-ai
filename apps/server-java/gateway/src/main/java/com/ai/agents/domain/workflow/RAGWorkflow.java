package com.ai.agents.domain.workflow;

import com.ai.agents.domain.WorkflowState;
import com.ai.agents.domain.service.agents.RagAgentService;

import java.util.*;

/**
 * RAG workflow domain model.
 * Manages document retrieval and knowledge base workflows.
 */
public final class RAGWorkflow {

    private final RagAgentService ragService;
    private final WorkflowState state;

    private RAGWorkflow(RagAgentService ragService, WorkflowState state) {
        this.ragService = Objects.requireNonNull(ragService, "RAGService cannot be null");
        this.state = Objects.requireNonNull(state, "WorkflowState cannot be null");
    }

    public RAGWorkflow(RagAgentService ragService) {
        this(ragService, WorkflowState.start("rag"));
    }

    private RAGWorkflow withState(WorkflowState newState) {
        return new RAGWorkflow(ragService, newState);
    }

    /**
     * Index document workflow.
     */
    public RAGWorkflow indexDocument(String content, String title, Map<String, String> metadata) {
        RagAgentService.Document doc = ragService.indexDocument(content, title, metadata);

        Map<String, Object> docData = new HashMap<>();
        docData.put("id", doc.id());
        docData.put("idValue", doc.idValue());
        docData.put("title", doc.title());
        docData.put("chunkCount", doc.chunkCount());

        return withState(state
                .updateState("document", docData)
                .updateState("phase", "index_document")
                .completeNode("index_document")
                .complete());
    }

    /**
     * Search workflow.
     */
    public RAGWorkflow search(String query, int topK) {
        List<RagAgentService.SearchResult> results = ragService.search(query, topK);

        List<Map<String, Object>> resultMaps = new ArrayList<>();
        for (RagAgentService.SearchResult result : results) {
            resultMaps.add(Map.of(
                    "docId", result.docId(),
                    "title", result.title(),
                    "chunk", result.chunk(),
                    "score", result.score(),
                    "chunkIndex", result.chunkIndex()
            ));
        }

        Map<String, Object> searchData = new HashMap<>();
        searchData.put("query", query);
        searchData.put("topK", topK);
        searchData.put("results", resultMaps);
        searchData.put("count", results.size());

        return withState(state
                .updateState("searchQuery", query)
                .updateState("searchResults", searchData)
                .updateState("phase", "search")
                .completeNode("search")
                .complete());
    }

    /**
     * Multi-hop retrieval workflow.
     */
    public RAGWorkflow multiHopSearch(String initialQuery, int hops) {
        RAGWorkflow current = withState(state.moveToNode("retrieve_initial"));

        List<RagAgentService.SearchResult> currentResults = ragService.search(initialQuery, 3);
        List<Map<String, Object>> allResults = new ArrayList<>();

        for (RagAgentService.SearchResult r : currentResults) {
            allResults.add(Map.of(
                    "docId", r.docId(),
                    "title", r.title(),
                    "chunk", r.chunk(),
                    "score", r.score(),
                    "hop", 0
            ));
        }

        current = current.withState(current.state
                .updateState("initialResults", allResults)
                .completeNode("retrieve_initial"));

        for (int i = 1; i < hops; i++) {
            current = current.withState(current.state.moveToNode("retrieve_hop_" + i));
            String refinedQuery = refineQuery(initialQuery, allResults);
            List<RagAgentService.SearchResult> hopResults = ragService.search(refinedQuery, 3);

            for (RagAgentService.SearchResult r : hopResults) {
                allResults.add(Map.of(
                        "docId", r.docId(),
                        "title", r.title(),
                        "chunk", r.chunk(),
                        "score", r.score(),
                        "hop", i
                ));
            }
            current = current.withState(current.state.completeNode("retrieve_hop_" + i));
        }

        return current.withState(current.state
                .updateState("allResults", allResults)
                .updateState("phase", "multi_hop")
                .complete());
    }

    private String refineQuery(String original, List<Map<String, Object>> context) {
        StringBuilder refined = new StringBuilder(original);
        for (Map<String, Object> result : context) {
            Object chunk = result.get("chunk");
            if (chunk != null) {
                refined.append(" ").append(chunk.toString().substring(0, Math.min(100, chunk.toString().length())));
            }
        }
        return refined.toString();
    }

    /**
     * Synthesize answer from retrieved context.
     */
    public RAGWorkflow synthesize(String userQuestion) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) state.getStateValue("allResults");
        if (results == null) {
            results = (List<Map<String, Object>>) state.getStateValue("searchResults");
        }

        StringBuilder context = new StringBuilder();
        if (results != null) {
            for (Map<String, Object> r : results) {
                context.append("- ").append(r.get("chunk")).append("\n");
            }
        }

        Map<String, Object> synthesis = new HashMap<>();
        synthesis.put("question", userQuestion);
        synthesis.put("contextCount", results != null ? results.size() : 0);
        synthesis.put("synthesized", true);

        return withState(state
                .updateState("synthesis", synthesis)
                .updateState("phase", "synthesize")
                .complete());
    }

    /**
     * Get search results.
     */
    @SuppressWarnings("unchecked")
    public List<RagAgentService.SearchResult> getResults() {
        List<Map<String, Object>> results = null;
        
        // Try to get results from searchResults map (if search was called)
        Map<String, Object> searchData = (Map<String, Object>) state.getStateValue("searchResults");
        if (searchData != null && searchData.containsKey("results")) {
            results = (List<Map<String, Object>>) searchData.get("results");
        }
        
        // Fallback to allResults (if multiHopSearch was called)
        if (results == null) {
            results = (List<Map<String, Object>>) state.getStateValue("allResults");
        }
        
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<RagAgentService.SearchResult> searchResults = new ArrayList<>();
        for (Map<String, Object> r : results) {
            searchResults.add(new RagAgentService.SearchResult(
                    (String) r.get("docId"),
                    (String) r.get("title"),
                    (String) r.get("chunk"),
                    ((Number) r.get("score")).doubleValue(),
                    ((Number) r.get("chunkIndex")).intValue()
            ));
        }
        return searchResults;
    }

    public WorkflowState state() { return state; }
    public boolean isCompleted() { return state.isCompleted(); }
    public String getPhase() { return (String) state.getStateValue("phase"); }
}
