package com.ai.billing.application;

import com.ai.billing.infrastructure.config.BillingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared in-process daily quota counter for HTTP filter and background automations.
 */
@Service
@EnableConfigurationProperties(BillingProperties.class)
public class DailyUsageQuotaService {

    private final BillingProperties properties;
    private final Map<String, DayCounter> counters = new ConcurrentHashMap<>();

    public DailyUsageQuotaService(BillingProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isQuotaEnabled();
    }

    public int dailyLimit() {
        return properties.dailyLimit();
    }

    public String plan() {
        return properties.getPlan();
    }

    /**
     * Atomically consumes one unit of daily quota when enabled.
     *
     * @return false when the client is over the daily limit
     */
    public boolean tryConsume(String clientId) {
        if (!properties.isQuotaEnabled()) {
            return true;
        }
        String day = LocalDate.now(ZoneOffset.UTC).toString();
        DayCounter counter = counters.compute(clientId, (k, existing) -> {
            if (existing == null || !existing.day.equals(day)) {
                return new DayCounter(day);
            }
            return existing;
        });
        int used = counter.count.incrementAndGet();
        if (used > properties.dailyLimit()) {
            counter.count.decrementAndGet();
            return false;
        }
        return true;
    }

    public int remaining(String clientId) {
        if (!properties.isQuotaEnabled()) {
            return properties.dailyLimit();
        }
        String day = LocalDate.now(ZoneOffset.UTC).toString();
        DayCounter counter = counters.get(clientId);
        if (counter == null || !counter.day.equals(day)) {
            return properties.dailyLimit();
        }
        return Math.max(0, properties.dailyLimit() - counter.count.get());
    }

    private static final class DayCounter {
        private final String day;
        private final AtomicInteger count = new AtomicInteger();

        private DayCounter(String day) {
            this.day = day;
        }
    }
}
