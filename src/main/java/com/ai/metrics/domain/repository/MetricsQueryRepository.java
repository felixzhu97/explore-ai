package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.vo.AiDomain;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Documentation. */
public interface MetricsQueryRepository {
  /** Documentation. */
  record LatencyStats(Double p50Ms, Double p95Ms) {}

  /** Documentation. */
  record TokenTotals(Long promptTokens, Long completionTokens) {}

  /** Documentation. */
  record NamedCount(String name, long count) {}

  /** Documentation. */
  record TimePoint(String day, long value) {}

  /** Documentation. */
  record ChatInventory(
      long sessionCount, long activeSessionCount, long messageCount, long webSourceReplyCount) {}

  /** Documentation. */
  record RagInventory(
      long documentCount,
      Map<String, Long> documentsByStatus,
      long chunkCount,
      long totalFileBytes) {}

  /** Documentation. */
  long countInvocations(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  long countErrors(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  LatencyStats latencyPercentiles(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  TokenTotals tokenTotals(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  List<NamedCount> countByDomain(Instant from, Instant to);

  /** Documentation. */
  List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  List<NamedCount> countByAgentType(Instant from, Instant to);

  /** Documentation. */
  List<NamedCount> topTools(Optional<AiDomain> domain, Instant from, Instant to, int limit);

  /** Documentation. */
  List<TimePoint> dailyRequests(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  List<TimePoint> dailyErrors(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  List<TimePoint> dailyLatencyP95(Optional<AiDomain> domain, Instant from, Instant to);

  /** Documentation. */
  List<TimePoint> dailySessionsCreated(Instant from, Instant to);

  /** Documentation. */
  List<TimePoint> dailyMessagesCreated(Instant from, Instant to);

  /** Documentation. */
  List<TimePoint> dailyDocumentsUploaded(Instant from, Instant to);

  /** Documentation. */
  ChatInventory chatInventory(Instant activeSince);

  /** Documentation. */
  RagInventory ragInventory();
}
