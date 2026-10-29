package com.ai.agent.controller.dto;

import com.ai.agent.domain.model.AgentRun;

/** Sync reply from a baseline Agent run. */
public record AgentInvokeResponse(String type, String message, String reply) {

  /** Maps a completed Agent Run. */
  public static AgentInvokeResponse createResponse(AgentRun run) {
    return new AgentInvokeResponse(run.getAgentType(), run.getMessage(), run.getReply());
  }
}
