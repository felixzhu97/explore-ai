package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.repository.MetricsQueryRepository.ChatInventory;

public record ChatInventoryResponse(
    long sessionCount, long activeSessionCount, long messageCount, long webSourceReplyCount)
    implements CapabilityInventoryResponse {

  /** Maps the chat inventory read from the metrics store. */
  public static ChatInventoryResponse from(ChatInventory inventory) {
    return new ChatInventoryResponse(
        inventory.getSessionCount(),
        inventory.getActiveSessionCount(),
        inventory.getMessageCount(),
        inventory.getWebSourceReplyCount());
  }
}
