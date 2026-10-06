package com.ai.billing.service;

import com.ai.billing.infra.config.BillingProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/** Shared in-process daily quota counter for HTTP filter and background automations. */
@Service
@EnableConfigurationProperties(BillingProperties.class)
public class DailyUsageQuotaService {

  private static final String GLOBAL_KEY = "global";
  private static final int MAX_TRACKED_KEYS = 100_000;

  private final BillingProperties properties;
  private final Cache<String, DayCounter> counters =
      Caffeine.newBuilder()
          .expireAfterAccess(Duration.ofDays(2))
          .maximumSize(MAX_TRACKED_KEYS)
          .build();

  public DailyUsageQuotaService(BillingProperties properties) {
    this.properties = properties;
  }

  /** Tells whether the daily quota is enforced. */
  public boolean isEnabled() {
    return properties.isQuotaEnabled();
  }

  /** Returns the daily request limit for the current plan. */
  public int getDailyLimit() {
    return properties.resolveDailyLimit();
  }

  /** Returns the configured billing plan. */
  public String getPlan() {
    return properties.getPlan();
  }

  /**
   * Atomically consumes one unit of the client's and the global daily quota when enabled.
   *
   * @return false when any limit is exhausted
   */
  public boolean tryConsume(String clientId) {
    return tryConsume(clientId, null);
  }

  /**
   * Atomically consumes one unit of the client, IP and global daily quota when enabled. Nothing is
   * consumed when any of them is exhausted.
   *
   * @param address client IP, or {@code null} to skip the per-IP limit
   * @return false when any limit is exhausted
   */
  public boolean tryConsume(String clientId, String address) {
    if (!properties.isQuotaEnabled()) {
      return true;
    }
    String day = today();
    List<DayCounter> consumed = new ArrayList<>(3);
    boolean allowed =
        consume(clientId, properties.resolveDailyLimit(), day, consumed)
            && (address == null
                || consume("ip:" + address, properties.resolveIpDailyLimit(), day, consumed))
            && (properties.getGlobalDailyRequests() <= 0
                || consume(GLOBAL_KEY, properties.getGlobalDailyRequests(), day, consumed));
    if (!allowed) {
      consumed.forEach(counter -> counter.count.decrementAndGet());
    }
    return allowed;
  }

  /** Returns the client's unused requests for the current UTC day. */
  public int countRemaining(String clientId) {
    if (!properties.isQuotaEnabled()) {
      return properties.resolveDailyLimit();
    }
    DayCounter counter = counters.getIfPresent(clientId);
    if (counter == null || !counter.day.equals(today())) {
      return properties.resolveDailyLimit();
    }
    return Math.max(0, properties.resolveDailyLimit() - counter.count.get());
  }

  private boolean consume(String key, int limit, String day, List<DayCounter> consumed) {
    DayCounter counter =
        counters
            .asMap()
            .compute(
                key,
                (k, existing) ->
                    existing == null || !existing.day.equals(day) ? new DayCounter(day) : existing);
    if (counter.count.incrementAndGet() > limit) {
      counter.count.decrementAndGet();
      return false;
    }
    consumed.add(counter);
    return true;
  }

  private static String today() {
    return LocalDate.now(ZoneOffset.UTC).toString();
  }

  private static final class DayCounter {
    private final String day;
    private final AtomicInteger count = new AtomicInteger();

    private DayCounter(String day) {
      this.day = day;
    }
  }
}
