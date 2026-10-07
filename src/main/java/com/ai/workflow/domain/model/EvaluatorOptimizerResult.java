package com.ai.workflow.domain.model;

import java.util.List;
import lombok.Value;

/** Final solution and chain-of-thought from evaluator-optimizer refinement. */
@Value
public class EvaluatorOptimizerResult {
  String solution;
  List<GenerationStep> chainOfThought;

  public EvaluatorOptimizerResult(String solution, List<GenerationStep> chainOfThought) {
    chainOfThought = chainOfThought == null ? List.of() : List.copyOf(chainOfThought);
    this.solution = solution;
    this.chainOfThought = chainOfThought;
  }
}
