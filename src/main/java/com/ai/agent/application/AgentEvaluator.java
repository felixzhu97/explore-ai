package com.ai.agent.application;

import com.ai.agent.domain.model.AgentEvaluation;
import com.ai.agent.domain.model.AgentPlan;

public interface AgentEvaluator {

    AgentEvaluation evaluate(String userMessage, AgentPlan plan, String agentResponse);
}
