package com.ai.agents.config;

import com.ai.agents.domain.agent.*;
import com.ai.agents.service.AgentRegistryService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runner that auto-registers all specialized agents at startup.
 */
@Component
public class AgentRegistrationRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistrationRunner.class);

    private final AgentRegistryService agentRegistryService;
    private final ChatAgent chatAgent;
    private final RagAgent ragAgent;
    private final TtsAgent ttsAgent;
    private final VisionAgent visionAgent;
    private final CodeAgent codeAgent;
    private final MonitorAgent monitorAgent;
    private final K8sAgent k8sAgent;

    public AgentRegistrationRunner(
            AgentRegistryService agentRegistryService,
            ChatAgent chatAgent,
            RagAgent ragAgent,
            TtsAgent ttsAgent,
            VisionAgent visionAgent,
            CodeAgent codeAgent,
            MonitorAgent monitorAgent,
            K8sAgent k8sAgent
    ) {
        this.agentRegistryService = agentRegistryService;
        this.chatAgent = chatAgent;
        this.ragAgent = ragAgent;
        this.ttsAgent = ttsAgent;
        this.visionAgent = visionAgent;
        this.codeAgent = codeAgent;
        this.monitorAgent = monitorAgent;
        this.k8sAgent = k8sAgent;
    }

    @PostConstruct
    public void registerAgents() {
        log.info("Registering specialized agents...");

        // Register specialized agents
        registerAgent(chatAgent);
        registerAgent(ragAgent);
        registerAgent(ttsAgent);
        registerAgent(visionAgent);
        registerAgent(codeAgent);
        registerAgent(monitorAgent);
        registerAgent(k8sAgent);

        log.info("Registered {} total agents", agentRegistryService.count());
    }

    private void registerAgent(AiAgent agent) {
        agentRegistryService.register(agent);
        log.debug("Registered agent: {} ({})", agent.getName(), agent.getAgentType());
    }
}
