package com.ai.agents.presentation.controller;

import com.ai.agents.application.service.*;
import com.ai.agents.domain.AgentType;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST controller for AI agent operations.
 * Thin presentation layer - delegates to application services.
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentOrchestrationService orchestrationService;
    private final K8sUseCase k8sUseCase;
    private final AIOpsUseCase aiOpsUseCase;
    private final LLMOpsUseCase llmOpsUseCase;
    private final PipelineUseCase pipelineUseCase;
    private final RagUseCase ragUseCase;
    private final MonitoringUseCase monitoringUseCase;
    private final VectorUseCase vectorUseCase;

    public AgentController(
            AgentOrchestrationService orchestrationService,
            K8sUseCase k8sUseCase,
            AIOpsUseCase aiOpsUseCase,
            LLMOpsUseCase llmOpsUseCase,
            PipelineUseCase pipelineUseCase,
            RagUseCase ragUseCase,
            MonitoringUseCase monitoringUseCase,
            VectorUseCase vectorUseCase
    ) {
        this.orchestrationService = orchestrationService;
        this.k8sUseCase = k8sUseCase;
        this.aiOpsUseCase = aiOpsUseCase;
        this.llmOpsUseCase = llmOpsUseCase;
        this.pipelineUseCase = pipelineUseCase;
        this.ragUseCase = ragUseCase;
        this.monitoringUseCase = monitoringUseCase;
        this.vectorUseCase = vectorUseCase;
    }

    // ========================================================================
    // General Agent Endpoints
    // ========================================================================

    @PostMapping("/chat")
    public Mono<ResponseEntity<Map<String, Object>>> chat(@Valid @RequestBody AgentRequestDto request) {
        log.info("Received chat request: type={}, message={}",
                request.agentType(), truncate(request.message(), 50));

        return orchestrationService.chat(request)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "message", response.message() != null ? response.message() : request.message(),
                        "agentType", response.agentType().name(),
                        "metadata", response.metadata() != null ? response.metadata() : Map.of()
                )))
                .onErrorResume(e -> {
                    log.error("Error processing chat request", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                            "error", "Failed to process request: " + e.getMessage()
                    )));
                });
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<String>> chatStream(@Valid @RequestBody AgentRequestDto request) {
        log.info("Received streaming chat request: type={}", request.agentType());

        return orchestrationService.chatStream(request)
                .map(response -> ResponseEntity.ok("data: " + (response.message() != null ? response.message() : "") + "\n\n"))
                .onErrorResume(e -> {
                    log.error("Error in streaming chat", e);
                    return Mono.just(ResponseEntity.ok("data: [ERROR] " + e.getMessage() + "\n\n"));
                });
    }

    @PostMapping("/invoke/{agentType}")
    public Mono<ResponseEntity<Map<String, Object>>> invoke(
            @PathVariable String agentType,
            @Valid @RequestBody AgentRequestDto request
    ) {
        log.info("Direct invocation: agent={}, message={}", agentType, truncate(request.message(), 50));

        AgentType targetType = AgentType.fromId(agentType);

        return orchestrationService.chatDirect(request, targetType)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "message", response.message() != null ? response.message() : "",
                        "agentType", response.agentType().name(),
                        "error", response.error() != null ? response.error() : ""
                )))
                .onErrorResume(e -> {
                    log.error("Error invoking agent", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                            "error", "Agent invocation failed: " + e.getMessage()
                    )));
                });
    }

    @PostMapping("/supervisor/invoke")
    public Mono<ResponseEntity<Map<String, Object>>> supervisorInvoke(@Valid @RequestBody AgentRequestDto request) {
        log.info("Supervisor invocation: message={}", truncate(request.message(), 50));

        return orchestrationService.chat(request)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "message", response.message() != null ? response.message() : "",
                        "agentType", response.agentType().name(),
                        "metadata", response.metadata() != null ? response.metadata() : Map.of()
                )));
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAgents() {
        var agents = orchestrationService.listAgents();
        return ResponseEntity.ok(Map.of(
                "agents", agents,
                "count", agents.size()
        ));
    }

    @GetMapping("/agents")
    public ResponseEntity<Map<String, Object>> listAllAgents() {
        List<Map<String, String>> agents = List.of(
                Map.of("name", "supervisor", "description", "Routes requests to specialized agents"),
                Map.of("name", "k8s", "description", "Kubernetes cluster management"),
                Map.of("name", "aiops", "description", "Incident management and root cause analysis"),
                Map.of("name", "llmops", "description", "ML model lifecycle management"),
                Map.of("name", "pipeline", "description", "DAG pipeline orchestration"),
                Map.of("name", "rag", "description", "Document retrieval and knowledge base"),
                Map.of("name", "monitoring", "description", "Metrics and alerting"),
                Map.of("name", "vector", "description", "Vector database operations"),
                Map.of("name", "model", "description", "ML model registry and versioning"),
                Map.of("name", "tts", "description", "Text-to-speech synthesis with multiple providers (Edge, Azure, Google, ElevenLabs)"),
                Map.of("name", "vision", "description", "Vision AI: object detection (YOLO), captioning (BLIP), OCR, image generation (Stable Diffusion), video generation")
        );
        return ResponseEntity.ok(Map.of("agents", agents, "count", agents.size()));
    }

    @GetMapping("/{agentType}")
    public ResponseEntity<Map<String, Object>> getAgent(@PathVariable String agentType) {
        AgentType type = AgentType.fromId(agentType);

        return orchestrationService.getAgent(type)
                .map(info -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", info.id(),
                        "name", info.name(),
                        "description", info.description(),
                        "status", info.status()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{agentType}/health")
    public ResponseEntity<Map<String, Object>> checkAgentHealth(@PathVariable String agentType) {
        AgentType type = AgentType.fromId(agentType);
        boolean available = orchestrationService.isAgentAvailable(type);

        return ResponseEntity.ok(Map.of(
                "agentType", agentType,
                "available", available,
                "status", available ? "UP" : "DOWN"
        ));
    }

    // ========================================================================
    // K8s Agent Endpoints
    // ========================================================================

    @GetMapping("/k8s/pods")
    public Mono<ResponseEntity<Map<String, Object>>> listPods(
            @RequestParam(defaultValue = "default") String namespace
    ) {
        return k8sUseCase.listPods(namespace)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/k8s/pods/{podName}")
    public Mono<ResponseEntity<Map<String, Object>>> getPod(
            @PathVariable String podName,
            @RequestParam(defaultValue = "default") String namespace
    ) {
        return k8sUseCase.getPod(podName, namespace)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/k8s/pods/{podName}/logs")
    public Mono<ResponseEntity<Map<String, Object>>> getPodLogs(
            @PathVariable String podName,
            @RequestParam(defaultValue = "default") String namespace,
            @RequestParam(defaultValue = "50") int lines
    ) {
        return k8sUseCase.getPodLogs(podName, namespace, lines)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/k8s/deployments")
    public Mono<ResponseEntity<Map<String, Object>>> listDeployments(
            @RequestParam(defaultValue = "default") String namespace
    ) {
        return k8sUseCase.listDeployments(namespace)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/k8s/services")
    public Mono<ResponseEntity<Map<String, Object>>> listServices(
            @RequestParam(defaultValue = "default") String namespace
    ) {
        return k8sUseCase.listServices(namespace)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/k8s/nodes")
    public Mono<ResponseEntity<Map<String, Object>>> getNodeStatus() {
        return k8sUseCase.getNodeStatus()
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @PostMapping("/k8s/deployments/{name}/scale")
    public Mono<ResponseEntity<Map<String, Object>>> scaleDeployment(
            @PathVariable String name,
            @RequestParam int replicas,
            @RequestParam(defaultValue = "default") String namespace
    ) {
        return k8sUseCase.scaleDeployment(name, replicas, namespace)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    // ========================================================================
    // AIOps Agent Endpoints
    // ========================================================================

    @PostMapping("/aiops/detect-anomaly")
    public Mono<ResponseEntity<Map<String, Object>>> detectAnomaly(
            @RequestParam String metric,
            @RequestParam(defaultValue = "1h") String timeRange,
            @RequestParam(defaultValue = "0.5") double sensitivity
    ) {
        return aiOpsUseCase.detectAnomaly(metric, timeRange, sensitivity)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @PostMapping("/aiops/incidents")
    public Mono<ResponseEntity<Map<String, Object>>> createIncident(
            @RequestParam String title,
            @RequestParam String severity,
            @RequestParam String description,
            @RequestParam List<String> affectedSystems
    ) {
        return aiOpsUseCase.createIncident(title, severity, description, affectedSystems)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/aiops/incidents")
    public Mono<ResponseEntity<Map<String, Object>>> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity
    ) {
        return aiOpsUseCase.listIncidents(status, severity)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/aiops/health")
    public Mono<ResponseEntity<Map<String, Object>>> getSystemHealth() {
        return aiOpsUseCase.getSystemHealth()
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @PostMapping("/aiops/root-cause")
    public Mono<ResponseEntity<Map<String, Object>>> rootCauseAnalysis(
            @RequestParam String incidentId,
            @RequestParam(required = false) List<String> affectedServices
    ) {
        return aiOpsUseCase.rootCauseAnalysis(incidentId, affectedServices)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @PostMapping("/aiops/incident-workflow")
    public Mono<ResponseEntity<Map<String, Object>>> runIncidentWorkflow(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String severity,
            @RequestParam List<String> affectedSystems
    ) {
        return aiOpsUseCase.runIncidentResponseWorkflow(title, description, severity, affectedSystems)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    // ========================================================================
    // LLMOps Agent Endpoints
    // ========================================================================

    @PostMapping("/llmops/models")
    public Mono<ResponseEntity<Map<String, Object>>> registerModel(
            @RequestParam String modelName,
            @RequestParam String version,
            @RequestParam String artifactUri
    ) {
        return llmOpsUseCase.registerModel(modelName, version, artifactUri)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/llmops/models")
    public Mono<ResponseEntity<Map<String, Object>>> listModels(
            @RequestParam(required = false) String modelName
    ) {
        return llmOpsUseCase.listModels(modelName)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @PostMapping("/llmops/models/{modelName}/deploy")
    public Mono<ResponseEntity<Map<String, Object>>> deployModel(
            @PathVariable String modelName,
            @RequestParam String version
    ) {
        return llmOpsUseCase.deployToProduction(modelName, version)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @PostMapping("/llmops/models/{modelName}/rollback")
    public Mono<ResponseEntity<Map<String, Object>>> rollbackModel(@PathVariable String modelName) {
        return llmOpsUseCase.rollback(modelName)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    // ========================================================================
    // Pipeline Agent Endpoints
    // ========================================================================

    @PostMapping("/pipeline/runs")
    public Mono<ResponseEntity<Map<String, Object>>> createPipelineRun(
            @RequestParam String pipelineName,
            @RequestParam List<String> steps
    ) {
        return pipelineUseCase.createRun(pipelineName, steps)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/pipeline/runs")
    public Mono<ResponseEntity<Map<String, Object>>> listPipelineRuns(
            @RequestParam(required = false) String pipelineName
    ) {
        return pipelineUseCase.listRuns(pipelineName)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/pipeline/runs/{runId}")
    public Mono<ResponseEntity<Map<String, Object>>> getPipelineRun(@PathVariable String runId) {
        return pipelineUseCase.getRun(runId)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    // ========================================================================
    // RAG Agent Endpoints
    // ========================================================================

    @PostMapping("/rag/index")
    public Mono<ResponseEntity<Map<String, Object>>> indexDocument(
            @RequestParam String content,
            @RequestParam String title
    ) {
        return ragUseCase.indexDocument(content, title, Map.of())
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/rag/search")
    public Mono<ResponseEntity<Map<String, Object>>> searchRag(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK
    ) {
        return ragUseCase.search(query, topK)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    // ========================================================================
    // Monitoring Agent Endpoints
    // ========================================================================

    @GetMapping("/monitoring/metrics")
    public Mono<ResponseEntity<Map<String, Object>>> queryMetrics(
            @RequestParam String metric,
            @RequestParam(defaultValue = "5m") String timeRange,
            @RequestParam(defaultValue = "avg") String aggregation
    ) {
        return monitoringUseCase.queryMetrics(metric, timeRange, aggregation)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @PostMapping("/monitoring/alerts")
    public Mono<ResponseEntity<Map<String, Object>>> createAlert(
            @RequestParam String name,
            @RequestParam String metric,
            @RequestParam String condition,
            @RequestParam double threshold
    ) {
        return monitoringUseCase.createAlert(name, metric, condition, threshold)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/monitoring/alerts")
    public Mono<ResponseEntity<Map<String, Object>>> listAlerts(
            @RequestParam(required = false) String status
    ) {
        return monitoringUseCase.listAlerts(status)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    // ========================================================================
    // Vector DB Agent Endpoints
    // ========================================================================

    @PostMapping("/vector/collections")
    public Mono<ResponseEntity<Map<String, Object>>> createCollection(
            @RequestParam String name,
            @RequestParam(defaultValue = "384") int dimension,
            @RequestParam(required = false) String description
    ) {
        return vectorUseCase.createCollection(name, dimension, description)
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    @GetMapping("/vector/collections")
    public Mono<ResponseEntity<Map<String, Object>>> listCollections() {
        return vectorUseCase.listCollections()
                .map(response -> ResponseEntity.ok(Map.<String, Object>of(
                        "result", response.message(),
                        "agentType", response.agentType().name()
                )));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
