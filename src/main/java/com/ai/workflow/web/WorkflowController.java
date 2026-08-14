package com.ai.workflow.web;

import com.ai.workflow.application.usecase.WorkflowUseCase;
import com.ai.workflow.domain.model.ChainResult;
import com.ai.workflow.domain.model.EvaluatorOptimizerResult;
import com.ai.workflow.domain.model.OrchestratorWorkersResult;
import com.ai.workflow.domain.model.ParallelizationResult;
import com.ai.workflow.domain.model.RoutingResult;
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

/** Documentation. */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

  private final WorkflowUseCase workflowUseCase;

  /** Documentation. */
  public WorkflowController(WorkflowUseCase workflowUseCase) {
    this.workflowUseCase = workflowUseCase;
  }

  /** Documentation. */
  @PostMapping("/chain")
  public ResponseEntity<ChainResult> chain(@Valid @RequestBody ChainWorkflowRequest request) {
    return ResponseEntity.ok(workflowUseCase.chain(request.userInput(), request.systemPrompts()));
  }

  /** Documentation. */
  @PostMapping("/parallel")
  public ResponseEntity<ParallelizationResult> parallel(
      @Valid @RequestBody ParallelizationWorkflowRequest request) {
    int parallelism = request.parallelism() == null ? 2 : request.parallelism();
    return ResponseEntity.ok(
        workflowUseCase.parallel(request.prompt(), request.items(), parallelism));
  }

  /** Documentation. */
  @PostMapping("/route")
  public ResponseEntity<RoutingResult> route(@Valid @RequestBody RoutingWorkflowRequest request) {
    return ResponseEntity.ok(workflowUseCase.route(request.input(), request.routes()));
  }

  /** Documentation. */
  @PostMapping("/orchestrator-workers")
  public ResponseEntity<OrchestratorWorkersResult> orchestratorWorkers(
      @Valid @RequestBody OrchestratorWorkersRequest request) {
    return ResponseEntity.ok(workflowUseCase.orchestratorWorkers(request.task()));
  }

  /** Documentation. */
  @PostMapping("/evaluator-optimizer")
  public ResponseEntity<EvaluatorOptimizerResult> evaluatorOptimizer(
      @Valid @RequestBody EvaluatorOptimizerRequest request) {
    return ResponseEntity.ok(workflowUseCase.evaluatorOptimizer(request.task()));
  }
}
