package com.ai.workflow.application;

import com.ai.workflow.domain.ChainResult;
import com.ai.workflow.domain.EvaluatorOptimizerResult;
import com.ai.workflow.domain.OrchestratorWorkersResult;
import com.ai.workflow.domain.ParallelizationResult;
import com.ai.workflow.domain.RoutingResult;
import com.ai.workflow.domain.ChainWorkflow;
import com.ai.workflow.domain.EvaluatorOptimizerWorkflow;
import com.ai.workflow.domain.OrchestratorWorkersWorkflow;
import com.ai.workflow.domain.ParallelizationWorkflow;
import com.ai.workflow.domain.RoutingWorkflow;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Application orchestration for workflow pattern endpoints.
 */
@Service
public class WorkflowUseCase {

    private final ChainWorkflow chainWorkflow;
    private final ParallelizationWorkflow parallelizationWorkflow;
    private final RoutingWorkflow routingWorkflow;
    private final OrchestratorWorkersWorkflow orchestratorWorkersWorkflow;
    private final EvaluatorOptimizerWorkflow evaluatorOptimizerWorkflow;

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
