package com.ai.agents.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Agent type enumeration.
 * Defines the types of agents available in the system.
 */
public enum AgentType {
    CHAT("chat", "Default chat agent for general conversational interactions"),
    RAG("rag", "Retrieval-Augmented Generation agent for document-aware Q&A"),
    TTS("tts", "Text-to-Speech agent for voice synthesis"),
    VISION("vision", "Vision agent for image analysis and OCR"),
    MEDIA("media", "Media generation agent for image creation"),
    TEXT("text", "Text processing agent for translation, summarization, etc."),
    SUPERVISOR("supervisor", "Supervisor agent for intent routing and orchestration");

    private final String id;
    private final String description;

    AgentType(String id, String description) {
        this.id = id;
        this.description = description;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AgentType fromId(String id) {
        if (id == null) {
            return CHAT;
        }
        for (AgentType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return CHAT;
    }

    public static AgentType fromOrdinal(int ordinal) {
        AgentType[] types = values();
        if (ordinal < 0 || ordinal >= types.length) {
            return CHAT;
        }
        return types[ordinal];
    }
}
