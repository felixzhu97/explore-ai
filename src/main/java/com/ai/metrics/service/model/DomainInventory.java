package com.ai.metrics.service.model;

import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.repository.MetricsQueryRepository.ChatInventory;
import com.ai.metrics.domain.repository.MetricsQueryRepository.RagInventory;
import java.util.List;

/** Domain-specific inventory shown on a metrics domain page; the shape depends on the domain. */
public sealed interface DomainInventory {

  record Chat(ChatInventory inventory) implements DomainInventory {}

  record Rag(RagInventory inventory) implements DomainInventory {}

  record Agents(AgentsHealth health) implements DomainInventory {}

  record Tools(List<NamedCount> topTools) implements DomainInventory {}

  /** Vision and workflow have no inventory beyond request totals. */
  record Requests(long requests, long errors) implements DomainInventory {}
}
