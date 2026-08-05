package com.ai.agent.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeepAgentProperties.class)
public class DeepAgentConfig {
}
