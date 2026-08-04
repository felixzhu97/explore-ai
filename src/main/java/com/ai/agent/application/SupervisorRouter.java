package com.ai.agent.application;

import com.ai.agent.domain.AgentDefinition;
import com.ai.agent.domain.RoutingPlan;

import java.util.List;

/**
 * Analyzes a user task and produces a routing plan for worker agents.
 */
public interface SupervisorRouter {

    RoutingPlan plan(String userMessage, List<AgentDefinition> workers);
}
