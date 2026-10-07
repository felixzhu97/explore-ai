package com.ai.workflow.domain.model;

import java.util.List;
import lombok.Value;

/** Ordered outputs from a parallelization workflow (same order as inputs). */
@Value
public class ParallelizationResult {
  List<String> outputs;

  public ParallelizationResult(List<String> outputs) {
    outputs = outputs == null ? List.of() : List.copyOf(outputs);
    this.outputs = outputs;
  }
}
