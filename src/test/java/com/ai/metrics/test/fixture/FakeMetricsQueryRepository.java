package com.ai.metrics.test.fixture;

import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationStats;
import com.ai.metrics.domain.vo.LatencyStats;
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
  public InvocationStats countInvocationStats(Optional<AiDomain> domain, Instant from, Instant to) {
    return new InvocationStats(requestCount, errorCount);
  }

  @Override
  public LatencyStats calculateLatencyPercentiles(
      Optional<AiDomain> domain, Instant from, Instant to) {
    return new LatencyStats(10.0, 40.0);
  }

  @Override
  public TokenTotals sumTokens(Optional<AiDomain> domain, Instant from, Instant to) {
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
  public List<NamedCount> listTopTools(
      Optional<AiDomain> domain, Instant from, Instant to, int limit) {
    return topTools;
  }

  @Override
  public List<TimePoint> countDailyRequests(Optional<AiDomain> domain, Instant from, Instant to) {
    return dailyRequests;
  }

  @Override
  public List<TimePoint> countDailyErrors(Optional<AiDomain> domain, Instant from, Instant to) {
    return dailyErrors;
  }

  @Override
  public List<TimePoint> calculateDailyLatencyP95(
      Optional<AiDomain> domain, Instant from, Instant to) {
    return dailyLatency;
  }

  @Override
  public List<TimePoint> countDailySessionsCreated(Instant from, Instant to) {
    return List.of();
  }

  @Override
  public List<TimePoint> countDailyMessagesCreated(Instant from, Instant to) {
    return List.of();
  }

  @Override
  public List<TimePoint> countDailyDocumentsUploaded(Instant from, Instant to) {
    return List.of();
  }

  @Override
  public ChatInventory getChatInventory(Instant activeSince) {
    return new ChatInventory(0, 0, 0, 0);
  }

  @Override
  public RagInventory getRagInventory() {
    return new RagInventory(0, documentsByStatus, 0, 0);
  }
}
