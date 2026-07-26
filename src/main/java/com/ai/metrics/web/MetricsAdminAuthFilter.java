package com.ai.metrics.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protects Metrics APIs with a shared admin key when configured.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html">OWASP REST Security</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 18)
@EnableConfigurationProperties(MetricsAdminProperties.class)
public class MetricsAdminAuthFilter extends OncePerRequestFilter {

    public static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    private final MetricsAdminProperties properties;

    public MetricsAdminAuthFilter(MetricsAdminProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isAuthEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/metrics");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(ADMIN_KEY_HEADER);
        if (!constantTimeEquals(provided, properties.getAdminApiKey())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    """
                    {"message":"Metrics admin key required","code":"METRICS_ADMIN_REQUIRED"}
                    """.strip().getBytes(StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }
}
