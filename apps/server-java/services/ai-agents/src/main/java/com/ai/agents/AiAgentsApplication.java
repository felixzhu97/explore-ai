package com.ai.agents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI Agents Service Application.
 * 
 * This module provides multi-agent orchestration capabilities using LangChain4j.
 * It includes:
 * - Supervisor Agent for request routing
 * - Specialized agents for different tasks (RAG, TTS, Vision, Code, etc.)
 */
@SpringBootApplication
@EnableConfigurationProperties
public class AiAgentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentsApplication.class, args);
    }
}
