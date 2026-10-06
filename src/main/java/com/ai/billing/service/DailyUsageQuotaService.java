package com.ai.billing.service;

import com.ai.billing.domain.vo.DailyUsage;
import com.ai.billing.domain.vo.QuotaDecision;
import com.ai.billing.domain.vo.QuotaPolicy;
import com.ai.billing.domain.vo.QuotaSubject;
import com.ai.common.domain.vo.OwnerKey;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Shared in-process daily quota counter for HTTP filter and background automations. */
@Service
@RequiredArgsConstructor
public class DailyUsageQuotaService {

  private static final int MAX_TRACKED_KEYS = 100_000;

  private final BillingPlanService billingPlanService;
  private final Cache<String, DailyUsage> counters =
      Caffeine.newBuilder()
          .expireAfterAccess(Duration.ofDays(2))
          .maximumSize(MAX_TRACKED_KEYS)
          .build();

  /** Tells whether the daily quota is enforced. */
  public boolean isEnabled() {
    return billingPlanService.currentPolicy().enforced();
  }

  /** Consumes one unit of the owner and global quota for background work that has no client IP. */
  public boolean tryConsume(OwnerKey owner) {
    return tryConsume(QuotaSubject.owner(owner), null).allowed();
  }

  /**
   * Atomically consumes one unit of the owner, IP and global daily quota. Nothing is consumed when
   * any of them is exhausted.
   *
   * @param owner owner counter, or {@code null} when the request has no Client Identity
   * @param address client IP, or {@code null} to skip the per-IP limit
   */
  public QuotaDecision tryConsume(QuotaSubject owner, String address) {
    QuotaPolicy policy = billingPlanService.currentPolicy();
    List<Charge> charges = new ArrayList<>(3);
    if (owner != null) {
      charges.add(new Charge(owner, policy.dailyLimit()));
    }
    if (address != null) {
      charges.add(new Charge(QuotaSubject.address(address), policy.ipDailyLimit()));
    }
    policy
        .globalDailyLimit()
        .ifPresent(limit -> charges.add(new Charge(QuotaSubject.GLOBAL, limit)));
    Charge shown = charges.isEmpty() ? null : charges.getFirst();
    int shownLimit = shown == null ? policy.dailyLimit() : shown.limit();
    if (!policy.enforced() || shown == null) {
      return QuotaDecision.allow(policy.plan(), shownLimit, shownLimit);
    }

    LocalDate today = today();
    List<Charge> consumed = new ArrayList<>(charges.size());
    for (Charge charge : charges) {
      if (!consume(charge, today)) {
        consumed.forEach(done -> release(done.subject(), today));
        return QuotaDecision.refuse(policy.plan(), shownLimit);
      }
      consumed.add(charge);
    }
    return QuotaDecision.allow(policy.plan(), shownLimit, remaining(shown, today));
  }

  private boolean consume(Charge charge, LocalDate today) {
    boolean[] granted = {false};
    counters
        .asMap()
        .compute(
            charge.subject().key(),
            (key, usage) -> {
              DailyUsage current = usage == null ? DailyUsage.none(today) : usage;
              if (current.remaining(charge.limit(), today) == 0) {
                return current;
              }
              granted[0] = true;
              return current.consume(today);
            });
    return granted[0];
  }

  private void release(QuotaSubject subject, LocalDate today) {
    counters.asMap().computeIfPresent(subject.key(), (key, usage) -> usage.release(today));
  }

  private int remaining(Charge charge, LocalDate today) {
    DailyUsage usage = counters.getIfPresent(charge.subject().key());
    return usage == null ? charge.limit() : usage.remaining(charge.limit(), today);
  }

  private static LocalDate today() {
    return LocalDate.now(ZoneOffset.UTC);
  }

  private record Charge(QuotaSubject subject, int limit) {}
}
