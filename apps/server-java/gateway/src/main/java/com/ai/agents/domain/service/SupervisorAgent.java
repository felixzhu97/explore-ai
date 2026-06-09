package com.ai.agents.domain.service;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.RoutingDecision;

import java.util.*;

/**
 * Supervisor Agent - Domain service for intent routing.
 * Routes user messages to the appropriate agent based on keywords.
 * 
 * This is a pure domain service with no framework dependencies.
 */
public final class SupervisorAgent {

    private static final Set<String> RAG_KEYWORDS = Set.of(
            "文档", "知识库", "检索", "rag", "search", "find", "查找", "查询",
            "document", "knowledge", "database"
    );

    private static final Set<String> TTS_KEYWORDS = Set.of(
            "语音", "朗读", "读出来", "text to speech", "tts", "speak",
            "audio", "声音", "speech", "语音合成"
    );

    private static final Set<String> VISION_KEYWORDS = Set.of(
            "图片", "图像", "图片分析", "vision", "image", "analyze", "识别",
            "看图", "图像识别", "照片"
    );

    private static final Set<String> MEDIA_KEYWORDS = Set.of(
            "生成图片", "画", "image generation", "generate image", "create image",
            "midjourney", "stable diffusion", "draw", "生成图像", "绘图"
    );

    private static final Set<String> TEXT_KEYWORDS = Set.of(
            "翻译", "翻译成", "translate", "summarize", "总结", "摘要",
            "grammar", "语法检查", "纠错", "polish", "润色", "rewrite", "改写"
    );

    private final Map<AgentType, Set<String>> routingKeywords;

    public SupervisorAgent() {
        this.routingKeywords = new EnumMap<>(AgentType.class);
        initializeKeywords();
    }

    private void initializeKeywords() {
        routingKeywords.put(AgentType.RAG, RAG_KEYWORDS);
        routingKeywords.put(AgentType.TTS, TTS_KEYWORDS);
        routingKeywords.put(AgentType.VISION, VISION_KEYWORDS);
        routingKeywords.put(AgentType.MEDIA, MEDIA_KEYWORDS);
        routingKeywords.put(AgentType.TEXT, TEXT_KEYWORDS);
    }

    /**
     * Route a message to the appropriate agent type.
     */
    public RoutingDecision route(String message) {
        if (message == null || message.isBlank()) {
            return RoutingDecision.fallback();
        }

        String lowerMessage = message.toLowerCase();

        // Check each agent type's keywords
        for (Map.Entry<AgentType, Set<String>> entry : routingKeywords.entrySet()) {
            AgentType type = entry.getKey();
            Set<String> keywords = entry.getValue();

            if (containsAny(lowerMessage, keywords)) {
                return RoutingDecision.to(type, 0.9, "Matched keyword from " + type.getId());
            }
        }

        // Default to chat
        return RoutingDecision.fallback();
    }

    /**
     * Route to a specific agent type.
     */
    public RoutingDecision routeTo(AgentType type, String message) {
        if (type != null && message != null && !message.isBlank()) {
            String lowerMessage = message.toLowerCase();
            Set<String> keywords = routingKeywords.get(type);

            if (keywords != null && containsAny(lowerMessage, keywords)) {
                return RoutingDecision.to(type, 1.0, "Explicit routing with keyword match");
            }
        }

        return RoutingDecision.of(type != null ? type : AgentType.CHAT);
    }

    /**
     * Check if text contains any of the keywords.
     */
    private boolean containsAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    /**
     * Get routing keywords for a specific type.
     */
    public Set<String> getKeywordsForType(AgentType type) {
        Set<String> keywords = routingKeywords.get(type);
        return keywords != null ? Collections.unmodifiableSet(keywords) : Set.of();
    }

    /**
     * Get all routing keywords.
     */
    public Map<AgentType, Set<String>> getAllKeywords() {
        Map<AgentType, Set<String>> result = new EnumMap<>(AgentType.class);
        for (Map.Entry<AgentType, Set<String>> entry : routingKeywords.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Add custom keywords for a type.
     */
    public void addKeywords(AgentType type, Set<String> keywords) {
        Set<String> existing = routingKeywords.computeIfAbsent(type, k -> new HashSet<>());
        existing.addAll(keywords);
    }

    /**
     * Remove keywords for a type.
     */
    public void removeKeywords(AgentType type, Set<String> keywords) {
        Set<String> existing = routingKeywords.get(type);
        if (existing != null) {
            existing.removeAll(keywords);
        }
    }
}
