package com.ai.metrics.test.fixture;

import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link MetricsQueryRepository} for service-layer unit tests. */
public class FakeMetricsQueryRepository implements MetricsQueryRepository {

  public long requestCount;
  public long errorCount;
  public List<NamedCount> byDomain = List.of();
  public List<NamedCount> topTools = List.of();
  public List<NamedCount> byModel = List.of();
  public List<NamedCount> byAgent = List.of();
  public List<TimePoint> dailyRequests = List.of();
  public List<TimePoint> dailyErrors = List.of();
  public List<TimePoint> dailyLatency = List.of();
  public final Map<String, Long> documentsByStatus = new LinkedHashMap<>();

  @Override
  public long countInvocations(Optional<AiDomain> domain, Instant from, Instant to) {
    return requestCount;
  }

  @Override
  public long countErrors(Optional<AiDomain> domain, Instant from, Instant to) {
    return errorCount;
  }

  @Override
  public LatencyStats latencyPercentiles(Optional<AiDomain> domain, Instant from, Instant to) {
    return new LatencyStats(10.0, 40.0);
  }

  @Override
  public TokenTotals tokenTotals(Optional<AiDomain> domain, Instant from, Instant to) {
    return new TokenTotals(11L, 22L);
  }

  @Override
  public List<NamedCount> countByDomain(Instant from, Instant to) {
    return byDomain;
  }

  @Override
  public List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to) {
    return byModel;
  }

  @Override
  public List<NamedCount> countByAgentType(Instant from, Instant to) {
    return byAgent;
  }

  @Override
  public List<NamedCount> topTools(Optional<AiDomain> domain, Instant from, Instant to, int limit) {
    return topTools;
  }

  @Override
  public List<TimePoint> dailyRequests(Optional<AiDomain> domain, Instant from, Instant to) {
    return dailyRequests;
  }

  @Override
  public List<TimePoint> dailyErrors(Optional<AiDomain> domain, Instant from, Instant to) {
    return dailyErrors;
  }

  @Override
  public List<TimePoint> dailyLatencyP95(Optional<AiDomain> domain, Instant from, Instant to) {
    return dailyLatency;
  }

  @Override
  public List<TimePoint> dailySessionsCreated(Instant from, Instant to) {
    return List.of();
  }

  @Override
  public List<TimePoint> dailyMessagesCreated(Instant from, Instant to) {
    return List.of();
  }

  @Override
  public List<TimePoint> dailyDocumentsUploaded(Instant from, Instant to) {
    return List.of();
  }

  @Override
  public ChatInventory chatInventory(Instant activeSince) {
    return new ChatInventory(0, 0, 0, 0);
  }

  @Override
  public RagInventory ragInventory() {
    return new RagInventory(0, documentsByStatus, 0, 0);
  }
}
