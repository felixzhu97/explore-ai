package com.ai.workflow.service.usecase;

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
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Application orchestration for workflow pattern endpoints. */
@Service
public class WorkflowUseCase {

  private final ChainWorkflow chainWorkflow;
  private final ParallelizationWorkflow parallelizationWorkflow;
  private final RoutingWorkflow routingWorkflow;
  private final OrchestratorWorkersWorkflow orchestratorWorkersWorkflow;
  private final EvaluatorOptimizerWorkflow evaluatorOptimizerWorkflow;

  /** Documentation. */
  public WorkflowUseCase(
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

  /** Documentation. */
  public ChainResult chain(String userInput, String[] systemPrompts) {
    return chainWorkflow.chain(userInput, systemPrompts);
  }

  /** Documentation. */
  public ParallelizationResult parallel(String prompt, List<String> items, int parallelism) {
    return parallelizationWorkflow.parallel(prompt, items, parallelism);
  }

  /** Documentation. */
  public RoutingResult route(String input, Map<String, String> routes) {
    return routingWorkflow.route(input, routes);
  }

  /** Documentation. */
  public OrchestratorWorkersResult orchestratorWorkers(String task) {
    return orchestratorWorkersWorkflow.process(task);
  }

  /** Documentation. */
  public EvaluatorOptimizerResult evaluatorOptimizer(String task) {
    return evaluatorOptimizerWorkflow.loop(task);
  }
}
