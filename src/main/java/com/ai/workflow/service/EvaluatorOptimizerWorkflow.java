package com.ai.workflow.service;

import com.ai.workflow.domain.model.EvaluatorOptimizerResult;

/** Generator / evaluator loop until PASS or max iterations. */
public interface EvaluatorOptimizerWorkflow {
  /** Improves an answer until the evaluator accepts it. */
  EvaluatorOptimizerResult runLoop(String task);
}
