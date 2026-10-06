package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationStats;
import com.ai.metrics.domain.vo.LatencyStats;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-side repository for aggregated invocation counts, latencies, tokens, and inventories. */
public interface MetricsQueryRepository {
  /** Counts calls and failed calls in the time range in one read. */
  InvocationStats countInvocationStats(Optional<AiDomain> domain, Instant from, Instant to);

  /** Counts calls per domain. */
  List<NamedCount> countByDomain(Instant from, Instant to);

  /** Counts calls per model. */
  List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to);

  /** Counts calls per agent type. */
  List<NamedCount> countByAgentType(Instant from, Instant to);

  /** Calculates the latency percentiles. */
  LatencyStats calculateLatencyPercentiles(Optional<AiDomain> domain, Instant from, Instant to);

  /** Sums the prompt and completion tokens. */
  TokenTotals sumTokens(Optional<AiDomain> domain, Instant from, Instant to);

  /** Lists the most used tools. */
  List<NamedCount> listTopTools(Optional<AiDomain> domain, Instant from, Instant to, int limit);

  /** Counts calls per day. */
  List<TimePoint> countDailyRequests(Optional<AiDomain> domain, Instant from, Instant to);

  /** Counts failed calls per day. */
  List<TimePoint> countDailyErrors(Optional<AiDomain> domain, Instant from, Instant to);

  /** Calculates the p95 latency per day. */
  List<TimePoint> calculateDailyLatencyP95(Optional<AiDomain> domain, Instant from, Instant to);

  /** Counts new chat sessions per day. */
  List<TimePoint> countDailySessionsCreated(Instant from, Instant to);

  /** Counts new chat messages per day. */
  List<TimePoint> countDailyMessagesCreated(Instant from, Instant to);

  /** Counts uploaded documents per day. */
  List<TimePoint> countDailyDocumentsUploaded(Instant from, Instant to);

  /** Returns the chat session and message totals. */
  ChatInventory getChatInventory(Instant activeSince);

  /** Returns the document and chunk totals. */
  RagInventory getRagInventory();

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
