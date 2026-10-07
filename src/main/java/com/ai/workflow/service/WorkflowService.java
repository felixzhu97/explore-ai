package com.ai.workflow.service;

import com.ai.workflow.domain.model.ChainResult;
import com.ai.workflow.domain.model.EvaluatorOptimizerResult;
import com.ai.workflow.domain.model.OrchestratorWorkersResult;
import com.ai.workflow.domain.model.ParallelizationResult;
import com.ai.workflow.domain.model.RoutingResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Application orchestration for workflow pattern endpoints. */
@Service
@RequiredArgsConstructor
public class WorkflowService {

  private final ChainWorkflow chainWorkflow;
  private final ParallelizationWorkflow parallelizationWorkflow;
  private final RoutingWorkflow routingWorkflow;
  private final OrchestratorWorkersWorkflow orchestratorWorkersWorkflow;
  private final EvaluatorOptimizerWorkflow evaluatorOptimizerWorkflow;

  /** Runs the prompts one after another. */
  public ChainResult runChain(String userInput, String[] systemPrompts) {
    return chainWorkflow.runChain(userInput, systemPrompts);
  }

  /** Runs the prompt over the items in parallel. */
  public ParallelizationResult runParallel(String prompt, List<String> items, int parallelism) {
    return parallelizationWorkflow.runParallel(prompt, items, parallelism);
  }

  /** Sends the input to the best route. */
  public RoutingResult route(String input, Map<String, String> routes) {
    return routingWorkflow.route(input, routes);
  }

  /** Runs the orchestrator-workers workflow. */
  public OrchestratorWorkersResult runOrchestratorWorkers(String task) {
    return orchestratorWorkersWorkflow.process(task);
  }

  /** Runs the evaluator-optimizer workflow. */
  public EvaluatorOptimizerResult runEvaluatorOptimizer(String task) {
    return evaluatorOptimizerWorkflow.runLoop(task);
  }
}
