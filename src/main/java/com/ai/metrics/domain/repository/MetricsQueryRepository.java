package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.vo.AiDomain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MetricsQueryRepository {

    record LatencyStats(Double p50Ms, Double p95Ms) {
    }

    record TokenTotals(Long promptTokens, Long completionTokens) {
    }

    record NamedCount(String name, long count) {
    }

    record TimePoint(String day, long value) {
    }

    record ChatInventory(
            long sessionCount,
            long activeSessionCount,
            long messageCount,
            long webSourceReplyCount
    ) {
    }

    record RagInventory(
            long documentCount,
            Map<String, Long> documentsByStatus,
            long chunkCount,
            long totalFileBytes
    ) {
    }

    long countInvocations(Optional<AiDomain> domain, Instant from, Instant to);

    long countErrors(Optional<AiDomain> domain, Instant from, Instant to);

    LatencyStats latencyPercentiles(Optional<AiDomain> domain, Instant from, Instant to);

    TokenTotals tokenTotals(Optional<AiDomain> domain, Instant from, Instant to);

    List<NamedCount> countByDomain(Instant from, Instant to);

    List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to);

    List<NamedCount> countByAgentType(Instant from, Instant to);

    List<NamedCount> topTools(Optional<AiDomain> domain, Instant from, Instant to, int limit);

    List<TimePoint> dailyRequests(Optional<AiDomain> domain, Instant from, Instant to);

    List<TimePoint> dailyErrors(Optional<AiDomain> domain, Instant from, Instant to);

    List<TimePoint> dailyLatencyP95(Optional<AiDomain> domain, Instant from, Instant to);

    List<TimePoint> dailySessionsCreated(Instant from, Instant to);

    List<TimePoint> dailyMessagesCreated(Instant from, Instant to);

    List<TimePoint> dailyDocumentsUploaded(Instant from, Instant to);

    ChatInventory chatInventory(Instant activeSince);

    RagInventory ragInventory();
}
