package com.ai.agents.application.service;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.service.agents.RagAgentService;
import com.ai.agents.domain.workflow.RAGWorkflow;
import com.ai.agents.presentation.dto.AgentResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;

/**
 * Use case for RAG agent operations.
 */
@Service
public class RagUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagUseCase.class);

    private final RagAgentService domainService;

    public RagUseCase(RagAgentService domainService) {
        this.domainService = domainService;
    }

    public Mono<AgentResponseDto> indexDocument(String content, String title, Map<String, String> metadata) {
        log.info("Indexing document: {}", title);
        try {
        RagAgentService.Document doc = domainService.indexDocument(content, title, metadata);
            String output = String.format("Document indexed successfully.\nID: %s\nTitle: %s\nChunks: %d",
                    doc.id(), doc.title(), doc.chunkCount());
            return Mono.just(AgentResponseDto.success(output, AgentType.RAG));
        } catch (Exception e) {
            log.error("Error indexing document: {}", e.getMessage());
            return Mono.just(AgentResponseDto.error(e.getMessage()));
        }
    }

    public Mono<AgentResponseDto> search(String query, int topK) {
        log.info("Searching RAG: {}", query);
        try {
            RAGWorkflow workflow = new RAGWorkflow(domainService);
            workflow = workflow.search(query, topK); // reassign to updated workflow
            var results = workflow.getResults();

            StringBuilder output = new StringBuilder("Search Results:\n\n");
            for (var result : results) {
                output.append(String.format("[%.3f] %s\n%s...\n\n",
                        result.score(),
                        result.title(),
                        result.chunk().substring(0, Math.min(200, result.chunk().length()))
                ));
            }
            return Mono.just(AgentResponseDto.success(output.toString(), AgentType.RAG));
        } catch (Exception e) {
            log.error("Error searching: {}", e.getMessage());
            return Mono.just(AgentResponseDto.error(e.getMessage()));
        }
    }

    public Mono<AgentResponseDto> multiHopSearch(String query, int hops) {
        log.info("Multi-hop search: {} hops", hops);
        try {
            RAGWorkflow workflow = new RAGWorkflow(domainService);
            workflow.multiHopSearch(query, hops);
            var results = workflow.getResults();

            String output = String.format("Multi-hop Search Completed\nHops: %d\nResults: %d",
                    hops, results.size());
            return Mono.just(AgentResponseDto.success(output, AgentType.RAG));
        } catch (Exception e) {
            log.error("Error in multi-hop search: {}", e.getMessage());
            return Mono.just(AgentResponseDto.error(e.getMessage()));
        }
    }
}
