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

    // Keyword sets for each agent type (matching Python AGENT_ROUTING)
    private static final Set<String> VECTOR_KEYWORDS = Set.of(
            "vector", "embedding", "search", "collection", "chroma", "pinecone",
            "向量", "嵌入", "检索", "向量数据库"
    );

    private static final Set<String> KUBERNETES_KEYWORDS = Set.of(
            "k8s", "kubernetes", "pod", "deployment", "service", "cluster", 
            "scale", "namespace", "kubectl", "container", "容器", "集群",
            "nodes", "ingresses", "configmap", "secret", "pv", "pvc"
    );

    private static final Set<String> MONITORING_KEYWORDS = Set.of(
            "monitor", "metric", "log", "alert", "prometheus", "grafana",
            "elasticsearch", "observe", "监控", "日志", "告警", "指标",
            "dashboard", "tracing", "jaeger", "zipkin"
    );

    private static final Set<String> MODEL_KEYWORDS = Set.of(
            "model", "deploy", "ml", "training", "version", "rollback",
            "a/b", "canary", "模型", "部署", "机器学习", "训练",
            "registry", "serving", "inference", "batch"
    );

    private static final Set<String> RAG_KEYWORDS = Set.of(
            "rag", "document", "knowledge", "retrieval", "index", "chunk", "search",
            "query", "search docs", "文档", "知识库", "检索", "检索增强",
            "vector search", "similarity", "semantic search"
    );

    private static final Set<String> LLMOPS_KEYWORDS = Set.of(
            "llmops", "experiment", "train", "evaluate", "benchmark", "metrics",
            "llm ops", "hyperparameter", "tuning", "hpo", "autolog"
    );

    private static final Set<String> AIOPS_KEYWORDS = Set.of(
            "aiops", "anomaly", "incident", "root cause", "investigate", "alert",
            "ai ops", "运维", "异常", "故障", "根因", "调查", "巡检",
            "incident response", "runbook", "sla"
    );

    private static final Set<String> VIDEO_KEYWORDS = Set.of(
            "video", "generate video", "text-to-video", "text to video", "t2v",
            "animation", "clip", "视频", "生成视频", "文生视频", "动画",
            "video generation", "movie", "footage"
    );

    private static final Set<String> TTS_KEYWORDS = Set.of(
            "tts", "speech", "text to speech", "voice", "audio", "synthesize",
            "语音", "朗读", "读出来", "text-to-speech", "speak", "声音",
            "语音合成", "text2speech", "文字转语音"
    );

    private static final Set<String> VISION_KEYWORDS = Set.of(
            "图片", "图像", "图片分析", "vision", "image", "analyze", "识别",
            "看图", "图像识别", "照片", "ocr", "yolo", "detection", "caption"
    );

    private static final Set<String> MEDIA_KEYWORDS = Set.of(
            "生成图片", "画", "image generation", "generate image", "create image",
            "midjourney", "stable diffusion", "draw", "生成图像", "绘图",
            "image gen", "text to image", "text-to-image", "dalle"
    );

    private static final Set<String> TEXT_KEYWORDS = Set.of(
            "翻译", "翻译成", "translate", "summarize", "总结", "摘要",
            "grammar", "语法检查", "纠错", "polish", "润色", "rewrite", "改写",
            "translation", "summary", "paraphrase"
    );

    private static final Set<String> PIPELINE_KEYWORDS = Set.of(
            "pipeline", "dag", "workflow", "etl", "数据管道", "工作流",
            "orchestrate", "step", "airflow", " Prefect", "dagster"
    );

    private static final Set<String> FEATURE_STORE_KEYWORDS = Set.of(
            "feature", "feature store", "feature engineering", "特征", "特征工程",
            "training set", "serving", "offline", "online"
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
        routingKeywords.put(AgentType.SUPERVISOR, Set.of());
        routingKeywords.put(AgentType.K8S, KUBERNETES_KEYWORDS);
        routingKeywords.put(AgentType.MONITORING, MONITORING_KEYWORDS);
        routingKeywords.put(AgentType.MODEL, MODEL_KEYWORDS);
        routingKeywords.put(AgentType.LLM_OPS, LLMOPS_KEYWORDS);
        routingKeywords.put(AgentType.AI_OPS, AIOPS_KEYWORDS);
        routingKeywords.put(AgentType.VIDEO, VIDEO_KEYWORDS);
    }

    /**
     * Route a message to the appropriate agent type.
     */
    public RoutingDecision route(String message) {
        if (message == null || message.isBlank()) {
            return RoutingDecision.fallback();
        }

        String lowerMessage = message.toLowerCase();

        // Check specialized agent keywords first (in order of specificity)
        // These routes to the supervisor which coordinates specialized agents

        // Check AIOps keywords
        if (containsAny(lowerMessage, AIOPS_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.95, "Matched AI Ops keywords");
        }

        // Check Kubernetes keywords
        if (containsAny(lowerMessage, KUBERNETES_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.95, "Matched Kubernetes keywords");
        }

        // Check LLM Ops keywords
        if (containsAny(lowerMessage, LLMOPS_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.95, "Matched LLM Ops keywords");
        }

        // Check monitoring keywords
        if (containsAny(lowerMessage, MONITORING_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.95, "Matched monitoring keywords");
        }

        // Check model/ML keywords
        if (containsAny(lowerMessage, MODEL_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.95, "Matched model/ML keywords");
        }

        // Check vector DB keywords
        if (containsAny(lowerMessage, VECTOR_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.95, "Matched vector DB keywords");
        }

        // Check RAG keywords
        if (containsAny(lowerMessage, RAG_KEYWORDS)) {
            return RoutingDecision.to(AgentType.RAG, 0.95, "Matched RAG keywords");
        }

        // Check pipeline keywords
        if (containsAny(lowerMessage, PIPELINE_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.90, "Matched pipeline keywords");
        }

        // Check feature store keywords
        if (containsAny(lowerMessage, FEATURE_STORE_KEYWORDS)) {
            return RoutingDecision.to(AgentType.SUPERVISOR, 0.90, "Matched feature store keywords");
        }

        // Check video generation keywords
        if (containsAny(lowerMessage, VIDEO_KEYWORDS)) {
            return RoutingDecision.to(AgentType.MEDIA, 0.95, "Matched video generation keywords");
        }

        // Check TTS keywords
        if (containsAny(lowerMessage, TTS_KEYWORDS)) {
            return RoutingDecision.to(AgentType.TTS, 0.95, "Matched TTS keywords");
        }

        // Check generic agent keywords from base routingKeywords map
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
     * Uses substring matching. Multi-word keywords must appear exactly,
     * single-word keywords use contains() for flexibility.
     */
    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;
            if (text.contains(keyword)) return true;
        }
        return false;
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
