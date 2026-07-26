package com.ai.common.web;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-client sliding window rate limit for mutating chat APIs.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html">OWASP DoS</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
@EnableConfigurationProperties(RateLimitProperties.class)
public class ClientRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public ClientRateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String method = request.getMethod();
        if (!HttpMethod.POST.name().equals(method) && !HttpMethod.DELETE.name().equals(method)) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/")) {
            return true;
        }
        return !(path.startsWith("/api/sessions")
                || path.startsWith("/api/chat")
                || path.startsWith("/api/text/chat")
                || path.startsWith("/api/privacy"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Object attr = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
        String key = attr instanceof String clientId && !clientId.isBlank()
                ? clientId
                : "ip:" + request.getRemoteAddr();

        if (!allow(key)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            byte[] body = """
                    {"message":"Too many requests","code":"RATE_LIMITED"}
                    """.strip().getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allow(String key) {
        long now = System.currentTimeMillis();
        long windowMs = properties.getWindowSeconds() * 1000L;
        int limit = properties.getRequestsPerWindow();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startedAtMs >= windowMs) {
                return new Window(now, new AtomicInteger(0));
            }
            return existing;
        });
        return window.count.incrementAndGet() <= limit;
    }

    private record Window(long startedAtMs, AtomicInteger count) {
    }
}
