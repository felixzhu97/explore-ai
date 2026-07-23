package com.ai.workflow.domain.service;

import com.ai.workflow.domain.model.ParallelizationResult;

import java.util.List;

/**
 * Parallel LLM calls over independent items with ordered aggregation.
 */
public interface ParallelizationWorkflow {

    /**
     * @param prompt       shared instruction prepended to each item
     * @param items        independent inputs (order preserved in the result)
     * @param parallelism  fixed thread-pool size (&gt; 0)
     */
    ParallelizationResult parallel(String prompt, List<String> items, int parallelism);
}
