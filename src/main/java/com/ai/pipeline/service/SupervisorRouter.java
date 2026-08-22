package com.ai.pipeline.service;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.RoutingPlan;
import java.util.List;

/** Analyzes a user task and produces a routing plan for worker agents. */
public interface SupervisorRouter {
  /** Documentation. */
  RoutingPlan plan(String userMessage, List<AgentDefinition> workers);
}
