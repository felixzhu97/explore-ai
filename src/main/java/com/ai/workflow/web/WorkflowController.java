package com.ai.workflow.web;

import com.ai.workflow.application.WorkflowUseCase;
import com.ai.workflow.domain.ChainResult;
import com.ai.workflow.domain.EvaluatorOptimizerResult;
import com.ai.workflow.domain.OrchestratorWorkersResult;
import com.ai.workflow.domain.ParallelizationResult;
import com.ai.workflow.domain.RoutingResult;
import com.ai.workflow.web.ChainWorkflowRequest;
import com.ai.workflow.web.EvaluatorOptimizerRequest;
import com.ai.workflow.web.OrchestratorWorkersRequest;
import com.ai.workflow.web.ParallelizationWorkflowRequest;
import com.ai.workflow.web.RoutingWorkflowRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowUseCase workflowUseCase;

    public WorkflowController(WorkflowUseCase workflowUseCase) {
        this.workflowUseCase = workflowUseCase;
    }

    @PostMapping("/chain")
    public ResponseEntity<ChainResult> chain(@Valid @RequestBody ChainWorkflowRequest request) {
        return ResponseEntity.ok(workflowUseCase.chain(request.userInput(), request.systemPrompts()));
    }

    @PostMapping("/parallel")
    public ResponseEntity<ParallelizationResult> parallel(
            @Valid @RequestBody ParallelizationWorkflowRequest request) {
        int parallelism = request.parallelism() == null ? 2 : request.parallelism();
        return ResponseEntity.ok(
                workflowUseCase.parallel(request.prompt(), request.items(), parallelism));
    }

    @PostMapping("/route")
    public ResponseEntity<RoutingResult> route(@Valid @RequestBody RoutingWorkflowRequest request) {
        return ResponseEntity.ok(workflowUseCase.route(request.input(), request.routes()));
    }

    @PostMapping("/orchestrator-workers")
    public ResponseEntity<OrchestratorWorkersResult> orchestratorWorkers(
            @Valid @RequestBody OrchestratorWorkersRequest request) {
        return ResponseEntity.ok(workflowUseCase.orchestratorWorkers(request.task()));
    }

    @PostMapping("/evaluator-optimizer")
    public ResponseEntity<EvaluatorOptimizerResult> evaluatorOptimizer(
            @Valid @RequestBody EvaluatorOptimizerRequest request) {
        return ResponseEntity.ok(workflowUseCase.evaluatorOptimizer(request.task()));
    }
}
