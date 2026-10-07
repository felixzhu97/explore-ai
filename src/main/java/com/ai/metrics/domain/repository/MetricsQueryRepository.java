package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.InvocationStats;
import com.ai.metrics.domain.model.LatencyStats;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-side repository for aggregated invocation counts, latencies, tokens, and inventories. */
public interface MetricsQueryRepository {
  /** Counts calls and failed calls in the time range in one read. */
  InvocationStats countInvocationStats(Optional<AiCapability> capability, Instant from, Instant to);

  /** Counts calls per capability. */
  List<NamedCount> countByCapability(Instant from, Instant to);

  /** Counts calls per model. */
  List<NamedCount> countByModel(Optional<AiCapability> capability, Instant from, Instant to);

  /** Counts calls per agent type. */
  List<NamedCount> countByAgentType(Instant from, Instant to);

  /** Calculates the latency percentiles. */
  LatencyStats calculateLatencyPercentiles(
      Optional<AiCapability> capability, Instant from, Instant to);

  /** Sums the prompt and completion tokens. */
  TokenTotals sumTokens(Optional<AiCapability> capability, Instant from, Instant to);

  /** Lists the most used tools. */
  List<NamedCount> listTopTools(
      Optional<AiCapability> capability, Instant from, Instant to, int limit);

  /** Counts calls per day. */
  List<TimePoint> countDailyRequests(Optional<AiCapability> capability, Instant from, Instant to);

  /** Counts failed calls per day. */
  List<TimePoint> countDailyErrors(Optional<AiCapability> capability, Instant from, Instant to);

  /** Calculates the p95 latency per day. */
  List<TimePoint> calculateDailyLatencyP95(
      Optional<AiCapability> capability, Instant from, Instant to);

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
