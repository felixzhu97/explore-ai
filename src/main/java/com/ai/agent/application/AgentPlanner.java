package com.ai.agent.application;

import com.ai.agent.domain.model.AgentPlan;

public interface AgentPlanner {

    AgentPlan plan(String userMessage, String priorFeedback);
}
