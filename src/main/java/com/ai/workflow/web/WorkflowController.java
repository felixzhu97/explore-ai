package com.ai.workflow.web;

import com.ai.workflow.domain.model.ChainResult;
import com.ai.workflow.domain.model.EvaluatorOptimizerResult;
import com.ai.workflow.domain.model.OrchestratorWorkersResult;
import com.ai.workflow.domain.model.ParallelizationResult;
import com.ai.workflow.domain.model.RoutingResult;
import com.ai.workflow.domain.service.ChainWorkflow;
import com.ai.workflow.domain.service.EvaluatorOptimizerWorkflow;
import com.ai.workflow.domain.service.OrchestratorWorkersWorkflow;
import com.ai.workflow.domain.service.ParallelizationWorkflow;
import com.ai.workflow.domain.service.RoutingWorkflow;
import com.ai.workflow.web.dto.ChainWorkflowRequest;
import com.ai.workflow.web.dto.EvaluatorOptimizerRequest;
import com.ai.workflow.web.dto.OrchestratorWorkersRequest;
import com.ai.workflow.web.dto.ParallelizationWorkflowRequest;
import com.ai.workflow.web.dto.RoutingWorkflowRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final ChainWorkflow chainWorkflow;
    private final ParallelizationWorkflow parallelizationWorkflow;
    private final RoutingWorkflow routingWorkflow;
    private final OrchestratorWorkersWorkflow orchestratorWorkersWorkflow;
    private final EvaluatorOptimizerWorkflow evaluatorOptimizerWorkflow;

    public WorkflowController(
            ChainWorkflow chainWorkflow,
            ParallelizationWorkflow parallelizationWorkflow,
            RoutingWorkflow routingWorkflow,
            OrchestratorWorkersWorkflow orchestratorWorkersWorkflow,
            EvaluatorOptimizerWorkflow evaluatorOptimizerWorkflow) {
        this.chainWorkflow = chainWorkflow;
        this.parallelizationWorkflow = parallelizationWorkflow;
        this.routingWorkflow = routingWorkflow;
        this.orchestratorWorkersWorkflow = orchestratorWorkersWorkflow;
        this.evaluatorOptimizerWorkflow = evaluatorOptimizerWorkflow;
    }

    @PostMapping("/chain")
    public ResponseEntity<ChainResult> chain(@Valid @RequestBody ChainWorkflowRequest request) {
        return ResponseEntity.ok(chainWorkflow.chain(request.userInput(), request.systemPrompts()));
    }

    @PostMapping("/parallel")
    public ResponseEntity<ParallelizationResult> parallel(
            @Valid @RequestBody ParallelizationWorkflowRequest request) {
        int parallelism = request.parallelism() == null ? 2 : request.parallelism();
        return ResponseEntity.ok(
                parallelizationWorkflow.parallel(request.prompt(), request.items(), parallelism));
    }

    @PostMapping("/route")
    public ResponseEntity<RoutingResult> route(@Valid @RequestBody RoutingWorkflowRequest request) {
        return ResponseEntity.ok(routingWorkflow.route(request.input(), request.routes()));
    }

    @PostMapping("/orchestrator-workers")
    public ResponseEntity<OrchestratorWorkersResult> orchestratorWorkers(
            @Valid @RequestBody OrchestratorWorkersRequest request) {
        return ResponseEntity.ok(orchestratorWorkersWorkflow.process(request.task()));
    }

    @PostMapping("/evaluator-optimizer")
    public ResponseEntity<EvaluatorOptimizerResult> evaluatorOptimizer(
            @Valid @RequestBody EvaluatorOptimizerRequest request) {
        return ResponseEntity.ok(evaluatorOptimizerWorkflow.loop(request.task()));
    }
}
