package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG (Retrieval Augmented Generation) agent for document Q&A.
 */
@Component
public class RagAgent extends AbstractAiAgent {

    private static final String SYSTEM_PROMPT = """
            You are a RAG (Retrieval Augmented Generation) assistant specialized in answering questions based on retrieved documents.

            Guidelines:
            - Always base your answers on the provided context
            - If the answer is not in the context, say "Based on the provided documents, I cannot find..."
            - Cite relevant parts of the document when answering
            - Be precise and accurate
            """;

    public RagAgent() {
        super("rag", "RAG Agent", "Retrieval Augmented Generation for document Q&A", AgentType.RAG);
    }

    @Override
    public boolean canHandle(AgentRequest request) {
        String msg = request.message().toLowerCase();
        return msg.contains("document") || msg.contains("rag") ||
               msg.contains("knowledge") || msg.contains("search") ||
               msg.contains("query") || msg.contains("find");
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        // Placeholder for RAG functionality
        return """
                [RAG Response - LLM integration pending]

                Query: %s

                Note: In production, this would:
                1. Embed the query using the configured embedding model
                2. Search the vector database for relevant documents
                3. Build context from top-k results
                4. Generate an answer using the LLM with retrieved context
                """.formatted(request.message());
    }
}
