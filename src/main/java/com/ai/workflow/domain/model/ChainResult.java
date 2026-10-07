package com.ai.workflow.domain.model;

import java.util.List;
import lombok.Value;

/** Result of a prompt-chaining workflow: final output plus intermediate step outputs. */
@Value
public class ChainResult {
  String output;
  List<String> intermediateSteps;

  public ChainResult(String output, List<String> intermediateSteps) {
    intermediateSteps = intermediateSteps == null ? List.of() : List.copyOf(intermediateSteps);
    this.output = output;
    this.intermediateSteps = intermediateSteps;
  }
}
