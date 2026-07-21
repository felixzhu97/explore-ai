package com.ai.workflow.domain.model;

import java.util.List;

/**
 * Result of a prompt-chaining workflow: final output plus intermediate step outputs.
 */
public record ChainResult(String output, List<String> intermediateSteps) {

    public ChainResult {
        intermediateSteps = intermediateSteps == null ? List.of() : List.copyOf(intermediateSteps);
    }
}
