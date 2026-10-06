package com.ai.common.controller.filter;

import com.ai.common.controller.ClientIdentity;
import com.ai.common.infra.config.RateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window rate limit for every mutating API call, counted per client and per IP.
 *
 * @see <a
 *     href="https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html">OWASP
 *     DoS</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
@EnableConfigurationProperties(RateLimitProperties.class)
public class ClientRateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_TRACKED_KEYS = 100_000;
  private static final Set<String> MUTATING_METHODS =
      Set.of(
          HttpMethod.POST.name(),
          HttpMethod.PUT.name(),
          HttpMethod.PATCH.name(),
          HttpMethod.DELETE.name());

  private final RateLimitProperties properties;
  private final Clock clock;
  private final Cache<String, Window> windows;

  public ClientRateLimitFilter(RateLimitProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    this.windows =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(Math.max(1, properties.getWindowSeconds()) * 2L))
            .maximumSize(MAX_TRACKED_KEYS)
            .build();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!properties.isEnabled() || !MUTATING_METHODS.contains(request.getMethod())) {
      return true;
    }
    String path = request.getRequestURI();
    return path == null || !path.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Object attr = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
    String address = ClientIdentity.resolveLimitAddress(request);
    String clientKey =
        attr instanceof String clientId && !clientId.isBlank()
            ? clientId
            : "ip:" + request.getRemoteAddr();

    boolean allowed =
        allow(clientKey, properties.getRequestsPerWindow())
            && (address == null || allow("ip:" + address, properties.getIpRequestsPerWindow()));
    if (!allowed) {
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(properties.getWindowSeconds()));
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      byte[] body =
          """
                    {"message":"Too many requests","code":"RATE_LIMITED"}
                    """
              .strip()
              .getBytes(StandardCharsets.UTF_8);
      response.getOutputStream().write(body);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean allow(String key, int limit) {
    long now = clock.millis();
    long windowMs = properties.getWindowSeconds() * 1000L;
    Window window =
        windows
            .asMap()
            .compute(
                key,
                (k, existing) -> {
                  if (existing == null || now - existing.startedAtMs >= windowMs) {
                    return new Window(now, new AtomicInteger(0));
                  }
                  return existing;
                });
    return window.count.incrementAndGet() <= limit;
  }

  private record Window(long startedAtMs, AtomicInteger count) {}
}
