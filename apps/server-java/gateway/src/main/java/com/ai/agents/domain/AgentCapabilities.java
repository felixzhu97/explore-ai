package com.ai.agents.domain;

/**
 * Agent capabilities value object.
 * Defines what capabilities an agent supports.
 */
public final class AgentCapabilities {
    private final boolean supportsRag;
    private final boolean supportsTts;
    private final boolean supportsVision;
    private final boolean supportsMedia;
    private final boolean supportsText;
    private final boolean supportsChat;

    private AgentCapabilities(
            boolean supportsRag,
            boolean supportsTts,
            boolean supportsVision,
            boolean supportsMedia,
            boolean supportsText,
            boolean supportsChat
    ) {
        this.supportsRag = supportsRag;
        this.supportsTts = supportsTts;
        this.supportsVision = supportsVision;
        this.supportsMedia = supportsMedia;
        this.supportsText = supportsText;
        this.supportsChat = supportsChat;
    }

    public static AgentCapabilities of(AgentType type) {
        return switch (type) {
            case RAG -> new AgentCapabilities(true, false, false, false, false, false);
            case TTS -> new AgentCapabilities(false, true, false, false, false, false);
            case VISION -> new AgentCapabilities(false, false, true, false, false, false);
            case MEDIA -> new AgentCapabilities(false, false, false, true, false, false);
            case TEXT -> new AgentCapabilities(false, false, false, false, true, false);
            case CHAT -> new AgentCapabilities(false, false, false, false, false, true);
            case SUPERVISOR -> new AgentCapabilities(true, true, true, true, true, true);
        };
    }

    public static AgentCapabilities all() {
        return new AgentCapabilities(true, true, true, true, true, true);
    }

    public static AgentCapabilities none() {
        return new AgentCapabilities(false, false, false, false, false, false);
    }

    public boolean supports(AgentType type) {
        return switch (type) {
            case RAG -> supportsRag;
            case TTS -> supportsTts;
            case VISION -> supportsVision;
            case MEDIA -> supportsMedia;
            case TEXT -> supportsText;
            case CHAT -> supportsChat;
            case SUPERVISOR -> supportsRag && supportsTts && supportsVision && supportsMedia && supportsText && supportsChat;
        };
    }

    public boolean supportsRag() { return supportsRag; }
    public boolean supportsTts() { return supportsTts; }
    public boolean supportsVision() { return supportsVision; }
    public boolean supportsMedia() { return supportsMedia; }
    public boolean supportsText() { return supportsText; }
    public boolean supportsChat() { return supportsChat; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentCapabilities that = (AgentCapabilities) o;
        return supportsRag == that.supportsRag &&
                supportsTts == that.supportsTts &&
                supportsVision == that.supportsVision &&
                supportsMedia == that.supportsMedia &&
                supportsText == that.supportsText &&
                supportsChat == that.supportsChat;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(supportsRag, supportsTts, supportsVision, supportsMedia, supportsText, supportsChat);
    }
}
