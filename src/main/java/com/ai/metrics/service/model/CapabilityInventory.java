package com.ai.metrics.service.model;

import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.repository.MetricsQueryRepository.ChatInventory;
import com.ai.metrics.domain.repository.MetricsQueryRepository.RagInventory;
import java.util.List;

/**
 * Capability-specific inventory shown on a metrics capability page; the shape depends on the
 * capability.
 */
public sealed interface CapabilityInventory {

  record Chat(ChatInventory inventory) implements CapabilityInventory {}

  record Rag(RagInventory inventory) implements CapabilityInventory {}

  record Agents(AgentsHealth health) implements CapabilityInventory {}

  record Tools(List<NamedCount> topTools) implements CapabilityInventory {}

  /** Vision and workflow have no inventory beyond request totals. */
  record Requests(long requests, long errors) implements CapabilityInventory {}
}
