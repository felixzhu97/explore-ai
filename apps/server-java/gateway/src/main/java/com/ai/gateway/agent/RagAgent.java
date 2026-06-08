package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * RAG Agent implementation that delegates to external RAG service.
 * Provides document-aware Q&A capabilities via vector search and LLM.
 */
@Component
public class RagAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(RagAgent.class);
    private final WebClient webClient;

    public RagAgent(
            WebClient.Builder webClientBuilder,
            @Value("${ai.agent.services.rag.base-url}") String ragServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(ragServiceUrl).build();
    }

    @Override
    public String name() {
        return "RagAgent";
    }

    @Override
    public AgentType type() {
        return AgentType.RAG;
    }

    @Override
    public Mono<AgentResponse> process(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        String query = request.message();
        int topK = request.topK() != null ? request.topK() : 5;

        Map<String, Object> body = Map.of(
                "query", query,
                "top_k", topK
        );

        log.info("Processing RAG request: query='{}', topK={}", query, topK);

        return webClient.post()
                .uri("/api/rag/chat")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String answer = (String) response.getOrDefault("answer", "");
                    log.info("RAG response received successfully");
                    return AgentResponse.success(answer, type());
                })
                .onErrorResume(e -> {
                    log.error("RAG request failed: {}", e.getMessage());
                    return Mono.just(AgentResponse.error("RAG failed: " + e.getMessage()));
                });
    }

    @Override
    public boolean supports(AgentType type) {
        return type == AgentType.RAG;
    }
}
