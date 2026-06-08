package com.ai.rag.service;

import com.ai.rag.model.RagChatRequest;
import com.ai.rag.model.SourceDocument;
import com.ai.rag.store.QdrantEmbeddingStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Service for RAG (Retrieval Augmented Generation) chat operations.
 * Combines vector search with LLM for document-aware responses.
 */
@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private static final String SYSTEM_PROMPT = """
            You are a helpful AI assistant that answers questions based on the provided context.

            Guidelines:
            - Always base your answers on the provided context
            - If the answer is not in the context, clearly state that
            - Be precise and cite relevant parts when possible
            - Keep answers concise but thorough
            """;

    private static final String CONTEXT_TEMPLATE = """
            Context:
            %s

            Question: %s

            Please answer based on the context provided.
            """;

    private final VectorSearchService vectorSearchService;
    private final ChatModel chatModel;

    public RagChatService(
            VectorSearchService vectorSearchService,
            ChatModel chatModel
    ) {
        this.vectorSearchService = vectorSearchService;
        this.chatModel = chatModel;
    }

    /**
     * Stream chat response with RAG context.
     *
     * @param request Chat request
     * @return Flux of response chunks
     */
    public Flux<String> streamChat(RagChatRequest request) {
        return Flux.create(emitter -> {
            try {
                // 1. Retrieve relevant context
                List<String> contextChunks = vectorSearchService.searchSimilar(
                        request.query(),
                        request.topK()
                );

                if (contextChunks.isEmpty()) {
                    emitter.next("I don't have relevant information in my knowledge base to answer this question.");
                    emitter.complete();
                    return;
                }

                // 2. Build context string
                String context = String.join("\n\n---\n\n", contextChunks);

                // 3. Build prompt
                String prompt = String.format(CONTEXT_TEMPLATE, context, request.query());

                // 4. Generate response
                ChatResponse response = chatModel.chat(List.of(
                        SystemMessage.from(SYSTEM_PROMPT),
                        UserMessage.from(prompt)
                ));

                String answer = response.aiMessage().text();
                emitter.next(answer);
                emitter.complete();

            } catch (Exception e) {
                log.error("Error in stream chat", e);
                emitter.error(e);
            }
        });
    }

    /**
     * Non-streaming chat with RAG context.
     *
     * @param query User query
     * @param topK  Number of context chunks
     * @return Answer string
     */
    public String chat(String query, int topK) {
        List<String> contextChunks = vectorSearchService.searchSimilar(query, topK);

        if (contextChunks.isEmpty()) {
            return "I don't have relevant information in my knowledge base to answer this question.";
        }

        String context = String.join("\n\n---\n\n", contextChunks);
        String prompt = String.format(CONTEXT_TEMPLATE, context, query);

        ChatResponse response = chatModel.chat(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(prompt)
        ));

        return response.aiMessage().text();
    }

    /**
     * Chat with document filtering.
     *
     * @param query  User query
     * @param docIds List of document IDs to search within
     * @param topK   Number of context chunks
     * @return Answer string
     */
    public String chat(String query, List<String> docIds, int topK) {
        List<String> contextChunks = vectorSearchService.searchSimilar(query, docIds, topK);

        if (contextChunks.isEmpty()) {
            return "I don't have relevant information in the specified documents to answer this question.";
        }

        String context = String.join("\n\n---\n\n", contextChunks);
        String prompt = String.format(CONTEXT_TEMPLATE, context, query);

        ChatResponse response = chatModel.chat(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(prompt)
        ));

        return response.aiMessage().text();
    }

    /**
     * Search for source documents without generating a response.
     *
     * @param query User query
     * @param topK  Number of results
     * @return List of source documents
     */
    public List<SourceDocument> searchSources(String query, int topK) {
        return vectorSearchService.searchWithScores(query, topK).stream()
                .map(match -> new SourceDocument(
                        match.embedded() != null ? match.embedded().text() : "",
                        match.score()
                ))
                .toList();
    }

    /**
     * Get vector store statistics.
     *
     * @return Statistics map
     */
    public Map<String, Object> getStats() {
        return vectorSearchService.getStats();
    }
}
