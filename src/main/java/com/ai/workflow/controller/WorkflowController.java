package com.ai.workflow.controller;

import com.ai.workflow.controller.dto.ChainWorkflowRequest;
import com.ai.workflow.controller.dto.EvaluatorOptimizerRequest;
import com.ai.workflow.controller.dto.OrchestratorWorkersRequest;
import com.ai.workflow.controller.dto.ParallelizationWorkflowRequest;
import com.ai.workflow.controller.dto.RoutingWorkflowRequest;
import com.ai.workflow.domain.model.ChainResult;
import com.ai.workflow.domain.model.EvaluatorOptimizerResult;
import com.ai.workflow.domain.model.OrchestratorWorkersResult;
import com.ai.workflow.domain.model.ParallelizationResult;
import com.ai.workflow.domain.model.RoutingResult;
import com.ai.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

  private final WorkflowService workflowService;

  /** Runs the chain workflow. */
  @PostMapping("/chain")
  public ResponseEntity<ChainResult> runChain(@Valid @RequestBody ChainWorkflowRequest request) {
    return ResponseEntity.ok(
        workflowService.runChain(request.userInput(), request.systemPromptArray()));
  }

  /** Runs the parallel workflow. */
  @PostMapping("/parallel")
  public ResponseEntity<ParallelizationResult> runParallel(
      @Valid @RequestBody ParallelizationWorkflowRequest request) {
    int parallelism = request.parallelism() == null ? 2 : request.parallelism();
    return ResponseEntity.ok(
        workflowService.runParallel(request.prompt(), request.items(), parallelism));
  }

  /** Runs the routing workflow. */
  @PostMapping("/route")
  public ResponseEntity<RoutingResult> routeTask(
      @Valid @RequestBody RoutingWorkflowRequest request) {
    return ResponseEntity.ok(workflowService.routeTask(request.input(), request.routes()));
  }

  /** Runs the orchestrator-workers workflow. */
  @PostMapping("/orchestrator-workers")
  public ResponseEntity<OrchestratorWorkersResult> runOrchestratorWorkers(
      @Valid @RequestBody OrchestratorWorkersRequest request) {
    return ResponseEntity.ok(workflowService.runOrchestratorWorkers(request.task()));
  }

  /** Runs the evaluator-optimizer workflow. */
  @PostMapping("/evaluator-optimizer")
  public ResponseEntity<EvaluatorOptimizerResult> runEvaluatorOptimizer(
      @Valid @RequestBody EvaluatorOptimizerRequest request) {
    return ResponseEntity.ok(workflowService.runEvaluatorOptimizer(request.task()));
  }
}
