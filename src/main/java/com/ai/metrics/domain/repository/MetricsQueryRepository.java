package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.vo.AiDomain;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-side repository for aggregated invocation counts, latencies, tokens, and inventories. */
public interface MetricsQueryRepository {
  long countInvocations(Optional<AiDomain> domain, Instant from, Instant to);

  long countErrors(Optional<AiDomain> domain, Instant from, Instant to);

  List<NamedCount> countByDomain(Instant from, Instant to);

  List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to);

  List<NamedCount> countByAgentType(Instant from, Instant to);

  LatencyStats calculateLatencyPercentiles(Optional<AiDomain> domain, Instant from, Instant to);

  TokenTotals sumTokens(Optional<AiDomain> domain, Instant from, Instant to);

  List<NamedCount> listTopTools(Optional<AiDomain> domain, Instant from, Instant to, int limit);

  List<TimePoint> countDailyRequests(Optional<AiDomain> domain, Instant from, Instant to);

  List<TimePoint> countDailyErrors(Optional<AiDomain> domain, Instant from, Instant to);

  List<TimePoint> calculateDailyLatencyP95(Optional<AiDomain> domain, Instant from, Instant to);

  List<TimePoint> countDailySessionsCreated(Instant from, Instant to);

  List<TimePoint> countDailyMessagesCreated(Instant from, Instant to);

  List<TimePoint> countDailyDocumentsUploaded(Instant from, Instant to);

  ChatInventory getChatInventory(Instant activeSince);

  RagInventory getRagInventory();

  record LatencyStats(Double p50Ms, Double p95Ms) {}

  record TokenTotals(Long promptTokens, Long completionTokens) {}

  record NamedCount(String name, long count) {}

  record TimePoint(String day, long value) {}

  record ChatInventory(
      long sessionCount, long activeSessionCount, long messageCount, long webSourceReplyCount) {}

  record RagInventory(
      long documentCount,
      Map<String, Long> documentsByStatus,
      long chunkCount,
      long totalFileBytes) {}
}
