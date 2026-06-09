package com.ai.agents.application.service;

import com.ai.agents.application.dto.AgentRoutingResult;
import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.service.AgentRegistry;
import com.ai.agents.domain.service.SupervisorAgent;
import com.ai.agents.domain.service.agents.*;
import com.ai.agents.infrastructure.llm.SupervisorLLMRouter;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main orchestration service for AI agents.
 * Coordinates routing, agent selection, and execution.
 */
@Service
public class AgentOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrationService.class);

    private final RouteMessageUseCase routeMessageUseCase;
    private final ProcessAgentUseCase processAgentUseCase;
    private final AgentRegistry agentRegistry;
    private final SupervisorAgent supervisorAgent;
    private final SupervisorLLMRouter llmRouter;

    private final RagAgentService ragAgentService;
    private final K8sAgentService k8sAgentService;
    private final AIOpsAgentService aiOpsAgentService;
    private final LLMOpsAgentService llmOpsAgentService;
    private final PipelineAgentService pipelineAgentService;
    private final FeatureStoreAgentService featureStoreAgentService;
    private final MonitoringAgentService monitoringAgentService;
    private final VectorAgentService vectorAgentService;
    private final ModelAgentService modelAgentService;
    private final TTSAgentService ttsAgentService;
    private final VideoAgentService videoAgentService;

    public AgentOrchestrationService(
            RouteMessageUseCase routeMessageUseCase,
            ProcessAgentUseCase processAgentUseCase,
            AgentRegistry agentRegistry,
            SupervisorAgent supervisorAgent,
            SupervisorLLMRouter llmRouter,
            RagAgentService ragAgentService,
            K8sAgentService k8sAgentService,
            AIOpsAgentService aiOpsAgentService,
            LLMOpsAgentService llmOpsAgentService,
            PipelineAgentService pipelineAgentService,
            FeatureStoreAgentService featureStoreAgentService,
            MonitoringAgentService monitoringAgentService,
            VectorAgentService vectorAgentService,
            ModelAgentService modelAgentService,
            TTSAgentService ttsAgentService,
            VideoAgentService videoAgentService
    ) {
        this.routeMessageUseCase = routeMessageUseCase;
        this.processAgentUseCase = processAgentUseCase;
        this.agentRegistry = agentRegistry;
        this.supervisorAgent = supervisorAgent;
        this.llmRouter = llmRouter;
        this.ragAgentService = ragAgentService;
        this.k8sAgentService = k8sAgentService;
        this.aiOpsAgentService = aiOpsAgentService;
        this.llmOpsAgentService = llmOpsAgentService;
        this.pipelineAgentService = pipelineAgentService;
        this.featureStoreAgentService = featureStoreAgentService;
        this.monitoringAgentService = monitoringAgentService;
        this.vectorAgentService = vectorAgentService;
        this.modelAgentService = modelAgentService;
        this.ttsAgentService = ttsAgentService;
        this.videoAgentService = videoAgentService;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing AgentOrchestrationService");
        initializeDefaultAgents();
        log.info("Registered {} agents", agentRegistry.size());
    }

    private void initializeDefaultAgents() {
        agentRegistry.register("supervisor", "SupervisorAgent", AgentType.SUPERVISOR);
        agentRegistry.register("rag", "RAGAgent", AgentType.RAG);
        agentRegistry.register("k8s", "K8sAgent", AgentType.SUPERVISOR);
        agentRegistry.register("aiops", "AIOpsAgent", AgentType.SUPERVISOR);
        agentRegistry.register("llmops", "LLMOpsAgent", AgentType.SUPERVISOR);
        agentRegistry.register("pipeline", "PipelineAgent", AgentType.SUPERVISOR);
        agentRegistry.register("monitoring", "MonitoringAgent", AgentType.SUPERVISOR);
        agentRegistry.register("vector", "VectorAgent", AgentType.SUPERVISOR);
        agentRegistry.register("model", "ModelAgent", AgentType.SUPERVISOR);
        agentRegistry.register("tts", "TTSAgent", AgentType.TTS);
        agentRegistry.register("video", "VideoAgent", AgentType.MEDIA);
        agentRegistry.register("text", "TextAgent", AgentType.TEXT);
        agentRegistry.register("vision", "VisionAgent", AgentType.VISION);
        agentRegistry.register("chat", "ChatAgent", AgentType.CHAT);
    }

    public AgentRoutingResult route(String message) {
        return routeMessageUseCase.route(message);
    }

    public Mono<AgentResponseDto> chat(AgentRequestDto request) {
        return processAgentUseCase.process(request);
    }

    public Mono<AgentResponseDto> chatDirect(AgentRequestDto request, AgentType targetType) {
        return processAgentUseCase.processDirect(request, targetType);
    }

    public Mono<AgentResponseDto> chatStream(AgentRequestDto request) {
        return processAgentUseCase.process(request)
                .map(response -> AgentResponseDto.streaming(response.message(), response.agentType()));
    }

    public List<AgentInfo> listAgents() {
        return agentRegistry.getAllAgents().stream()
                .map(agent -> new AgentInfo(
                        agent.getIdValue(),
                        agent.getNameValue(),
                        agent.type().getDescription(),
                        agent.type().name().toLowerCase()
                ))
                .collect(Collectors.toList());
    }

    public Optional<AgentInfo> getAgent(AgentType type) {
        return agentRegistry.findByType(type)
                .map(agent -> new AgentInfo(
                        agent.getIdValue(),
                        agent.getNameValue(),
                        agent.type().getDescription(),
                        agent.type().name().toLowerCase()
                ));
    }

    public boolean isAgentAvailable(AgentType type) {
        return agentRegistry.hasAgent(type);
    }

    public List<AgentType> getAvailableTypes() {
        return processAgentUseCase.getAvailableTypes();
    }

    public SupervisorLLMRouter.RoutingExplanation explainRouting(String message) {
        return llmRouter.explain(message);
    }

    public record AgentInfo(
            String id,
            String name,
            String description,
            String status
    ) {}
}
