package com.ai.workflow.domain.model;

import java.util.List;

/**
 * Ordered outputs from a parallelization workflow (same order as inputs).
 */
public record ParallelizationResult(List<String> outputs) {

    public ParallelizationResult {
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
    }
}
