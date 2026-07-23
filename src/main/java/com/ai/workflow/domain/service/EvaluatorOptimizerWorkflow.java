package com.ai.workflow.domain.service;

import com.ai.workflow.domain.model.EvaluatorOptimizerResult;

/**
 * Generator / evaluator loop until PASS or max iterations.
 */
public interface EvaluatorOptimizerWorkflow {

    EvaluatorOptimizerResult loop(String task);
}
