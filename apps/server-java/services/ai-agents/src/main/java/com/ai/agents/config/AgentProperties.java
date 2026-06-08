package com.ai.agents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AI Agents.
 */
@Configuration
@ConfigurationProperties(prefix = "agents")
public class AgentProperties {

    private Llm llm = new Llm();
    private Supervisor supervisor = new Supervisor();
    private Routing routing = new Routing();

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    public static class Llm {
        private String provider = "deepseek";
        private String modelName = "deepseek-chat";
        private String apiKey = "";
        private String baseUrl = "https://api.deepseek.com";
        private double temperature = 0.7;
        private int maxTokens = 4096;
        private int timeoutSeconds = 120;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Supervisor {
        private String systemPrompt = """
                You are a Supervisor Agent that coordinates multiple specialized agents.

                Available agents:
                - rag: For document retrieval and question answering
                - tts: For text-to-speech synthesis
                - vision: For image analysis and generation
                - media: For media generation
                - text: For text processing
                - code: For code generation and analysis
                - data: For data analysis
                - monitor: For system monitoring
                - k8s: For Kubernetes operations
                - vector: For vector database operations
                - aiops: For incident response and operations

                Route user requests to the most appropriate agent based on the request content.
                Provide a brief explanation of why you chose this agent.
                """;

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }
    }

    public static class Routing {
        private boolean enableFallback = true;
        private String defaultAgent = "chat";

        public boolean isEnableFallback() {
            return enableFallback;
        }

        public void setEnableFallback(boolean enableFallback) {
            this.enableFallback = enableFallback;
        }

        public String getDefaultAgent() {
            return defaultAgent;
        }

        public void setDefaultAgent(String defaultAgent) {
            this.defaultAgent = defaultAgent;
        }
    }
}
