package com.ai.billing.controller;

import com.ai.billing.infra.config.BillingProperties;
import com.ai.billing.service.DailyUsageQuotaService;
import com.ai.common.controller.ClientIdentity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Free/Pro daily hard quota per Client Identity (commercial cost guardrail).
 *
 * @see <a href="https://docs.stripe.com/billing/subscriptions/usage-based">Stripe usage-based
 *     billing</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 28)
@EnableConfigurationProperties(BillingProperties.class)
public class UsageQuotaFilter extends OncePerRequestFilter {

  private final DailyUsageQuotaService dailyUsageQuotaService;

  /** Documentation. */
  public UsageQuotaFilter(DailyUsageQuotaService dailyUsageQuotaService) {
    this.dailyUsageQuotaService = dailyUsageQuotaService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!dailyUsageQuotaService.isEnabled()) {
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
        || path.startsWith("/api/pipelines")
        || path.startsWith("/api/automations")
        || path.startsWith("/api/skills")
        || path.startsWith("/api/tools")
        || path.startsWith("/api/images")
        || path.startsWith("/api/audio")
        || path.startsWith("/api/vision")
        || path.startsWith("/api/mcp"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Object attr = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
    String clientId =
        attr instanceof String id && !id.isBlank() ? id : "ip:" + request.getRemoteAddr();
    int limit = dailyUsageQuotaService.dailyLimit();
    final boolean allowed = dailyUsageQuotaService.tryConsume(clientId);
    response.setHeader("X-Quota-Limit", String.valueOf(limit));
    response.setHeader(
        "X-Quota-Remaining", String.valueOf(dailyUsageQuotaService.remaining(clientId)));
    response.setHeader("X-Quota-Plan", dailyUsageQuotaService.plan());
    if (!allowed) {
      response.setStatus(429);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response
          .getOutputStream()
          .write(
              """
                    {"message":"Daily plan quota exceeded","code":"QUOTA_EXCEEDED"}
                    """
                  .strip()
                  .getBytes(StandardCharsets.UTF_8));
      return;
    }
    filterChain.doFilter(request, response);
  }
}
