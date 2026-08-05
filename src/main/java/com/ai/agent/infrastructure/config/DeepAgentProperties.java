package com.ai.agent.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent")
public class DeepAgentProperties {

    private int maxIterations = 5;
    private String systemPrompt = """
            You are an autonomous research and analysis agent.
            Follow the current plan, use tools when they improve accuracy,
            and produce a clear answer for the user.
            """;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
