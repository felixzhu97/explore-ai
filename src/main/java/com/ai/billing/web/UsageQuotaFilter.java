package com.ai.billing.web;

import com.ai.billing.infrastructure.config.BillingProperties;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Free/Pro daily hard quota per Client Identity (commercial cost guardrail).
 *
 * @see <a href="https://docs.stripe.com/billing/subscriptions/usage-based">Stripe usage-based billing</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 28)
@EnableConfigurationProperties(BillingProperties.class)
public class UsageQuotaFilter extends OncePerRequestFilter {

    private final BillingProperties properties;
    private final Map<String, DayCounter> counters = new ConcurrentHashMap<>();

    public UsageQuotaFilter(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isQuotaEnabled()) {
            return true;
        }
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/")) {
            return true;
        }
        return !(path.startsWith("/api/chat")
                || path.startsWith("/api/text")
                || path.startsWith("/api/rag")
                || path.startsWith("/api/agents")
                || path.startsWith("/api/tools")
                || path.startsWith("/api/images")
                || path.startsWith("/api/audio")
                || path.startsWith("/api/vision")
                || path.startsWith("/api/mcp"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Object attr = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
        String clientId = attr instanceof String id && !id.isBlank()
                ? id
                : "ip:" + request.getRemoteAddr();
        String day = LocalDate.now(ZoneOffset.UTC).toString();
        DayCounter counter = counters.compute(clientId, (k, existing) -> {
            if (existing == null || !existing.day.equals(day)) {
                return new DayCounter(day);
            }
            return existing;
        });
        int used = counter.count.incrementAndGet();
        int limit = properties.dailyLimit();
        response.setHeader("X-Quota-Limit", String.valueOf(limit));
        response.setHeader("X-Quota-Remaining", String.valueOf(Math.max(0, limit - used)));
        response.setHeader("X-Quota-Plan", properties.getPlan());
        if (used > limit) {
            counter.count.decrementAndGet();
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    """
                    {"message":"Daily plan quota exceeded","code":"QUOTA_EXCEEDED"}
                    """.strip().getBytes(StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class DayCounter {
        private final String day;
        private final AtomicInteger count = new AtomicInteger();

        private DayCounter(String day) {
            this.day = day;
        }
    }
}
