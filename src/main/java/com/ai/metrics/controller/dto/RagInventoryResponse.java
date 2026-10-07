package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.repository.MetricsQueryRepository.RagInventory;
import java.util.Map;

/**
 * Knowledge base inventory.
 *
 * @param documentsByStatus document count keyed by document status name, e.g. {@code READY}
 */
public record RagInventoryResponse(
    long documentCount, Map<String, Long> documentsByStatus, long chunkCount, long totalFileBytes)
    implements CapabilityInventoryResponse {

  /** Maps the knowledge base inventory read from the metrics store. */
  public static RagInventoryResponse createResponse(RagInventory inventory) {
    return new RagInventoryResponse(
        inventory.getDocumentCount(),
        inventory.getDocumentsByStatus(),
        inventory.getChunkCount(),
        inventory.getTotalFileBytes());
  }
}
