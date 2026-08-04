package com.ai.workflow.domain;

import com.ai.workflow.domain.EvaluatorOptimizerResult;

/**
 * Generator / evaluator loop until PASS or max iterations.
 */
public interface EvaluatorOptimizerWorkflow {

    EvaluatorOptimizerResult loop(String task);
}
