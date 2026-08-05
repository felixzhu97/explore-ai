package com.ai.pipeline.application;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.RoutingPlan;

import java.util.List;

/**
 * Analyzes a user task and produces a routing plan for worker agents.
 */
public interface SupervisorRouter {

    RoutingPlan plan(String userMessage, List<AgentDefinition> workers);
}
