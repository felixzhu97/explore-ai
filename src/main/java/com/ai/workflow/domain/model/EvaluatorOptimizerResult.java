package com.ai.workflow.domain.model;

import java.util.List;

/**
 * Final solution and chain-of-thought from evaluator-optimizer refinement.
 */
public record EvaluatorOptimizerResult(String solution, List<GenerationStep> chainOfThought) {

    public EvaluatorOptimizerResult {
        chainOfThought = chainOfThought == null ? List.of() : List.copyOf(chainOfThought);
    }
}
