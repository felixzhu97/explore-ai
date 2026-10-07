package com.ai.workflow.service;

import com.ai.workflow.domain.model.ChainResult;

/** Prompt chaining: sequential LLM steps where each output feeds the next. */
public interface ChainWorkflow {

  /**
   * Runs {@code userInput} through {@code systemPrompts} in order.
   *
   * @param userInput initial user content
   * @param systemPrompts ordered step instructions; empty uses implementation defaults
   */
  ChainResult runChain(String userInput, String[] systemPrompts);
}
