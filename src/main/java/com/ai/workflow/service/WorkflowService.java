package com.ai.workflow.service;

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

  public ChainResult chain(String userInput, String[] systemPrompts) {
    return chainWorkflow.chain(userInput, systemPrompts);
  }

  public ParallelizationResult parallel(String prompt, List<String> items, int parallelism) {
    return parallelizationWorkflow.parallel(prompt, items, parallelism);
  }

  public RoutingResult route(String input, Map<String, String> routes) {
    return routingWorkflow.route(input, routes);
  }

  public OrchestratorWorkersResult orchestratorWorkers(String task) {
    return orchestratorWorkersWorkflow.process(task);
  }

  public EvaluatorOptimizerResult evaluatorOptimizer(String task) {
    return evaluatorOptimizerWorkflow.loop(task);
  }
}
