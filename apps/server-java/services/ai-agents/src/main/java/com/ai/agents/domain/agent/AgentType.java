package com.ai.agents.domain.agent;

/**
 * Enumeration of all available agent types.
 */
public enum AgentType {
    /**
     * Main supervisor agent that routes requests to specialized agents.
     */
    SUPERVISOR("supervisor", "Supervisor Agent"),

    /**
     * General chat agent for conversational interactions.
     */
    CHAT("chat", "Chat Agent"),

    /**
     * RAG (Retrieval Augmented Generation) agent for document Q&A.
     */
    RAG("rag", "RAG Agent"),

    /**
     * Text-to-Speech agent for synthesizing speech.
     */
    TTS("tts", "TTS Agent"),

    /**
     * Vision agent for image analysis and generation.
     */
    VISION("vision", "Vision Agent"),

    /**
     * Media generation agent for creating images/videos.
     */
    MEDIA("media", "Media Generation Agent"),

    /**
     * Text processing agent for NLP tasks.
     */
    TEXT("text", "Text Processing Agent"),

    /**
     * Code generation and analysis agent.
     */
    CODE("code", "Code Agent"),

    /**
     * Data analysis agent.
     */
    DATA("data", "Data Analysis Agent"),

    /**
     * Document processing agent.
     */
    DOC("doc", "Document Agent"),

    /**
     * Tool execution agent.
     */
    TOOL("tool", "Tool Agent"),

    /**
     * System monitoring agent.
     */
    MONITOR("monitor", "Monitor Agent"),

    /**
     * Kubernetes operations agent.
     */
    K8S("k8s", "Kubernetes Agent"),

    /**
     * Vector database operations agent.
     */
    VECTOR("vector", "Vector DB Agent"),

    /**
     * LLM operations agent.
     */
    LLMOPS("llmops", "LLMOps Agent"),

    /**
     * Feature store operations agent.
     */
    FEATURE_STORE("feature_store", "Feature Store Agent"),

    /**
     * Pipeline/orchestration agent.
     */
    PIPELINE("pipeline", "Pipeline Agent"),

    /**
     * AIOps/incident response agent.
     */
    AIOPS("aiops", "AIOps Agent"),

    /**
     * Video generation agent.
     */
    VIDEO("video", "Video Generation Agent");

    private final String id;
    private final String displayName;

    AgentType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Find agent type by its ID string.
     */
    public static AgentType fromId(String id) {
        for (AgentType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown agent type: " + id);
    }
}
