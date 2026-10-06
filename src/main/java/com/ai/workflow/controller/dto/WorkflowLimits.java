package com.ai.workflow.controller.dto;

/** Request size limits that cap how many LLM calls one workflow request can trigger. */
public final class WorkflowLimits {

  public static final int MAX_TEXT_LENGTH = 8_000;
  public static final int MAX_ITEMS = 20;
  public static final int MAX_PARALLELISM = 8;
  public static final int MAX_CHAIN_STEPS = 10;
  public static final int MAX_ROUTES = 20;

  private WorkflowLimits() {}
}
