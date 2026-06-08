package com.ai.agents.config;

import com.ai.agents.domain.agent.AiAgent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Configuration for auto-registering all available agents.
 */
@Component
public class AgentAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentAutoConfiguration.class);

    private final ApplicationContext applicationContext;

    public AgentAutoConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void registerAgents() {
        var agentBeans = applicationContext.getBeansOfType(AiAgent.class);
        log.info("Found {} agents: {}", agentBeans.size(), agentBeans.keySet());
    }
}
