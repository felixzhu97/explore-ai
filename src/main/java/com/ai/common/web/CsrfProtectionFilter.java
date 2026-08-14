package com.ai.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SPA CSRF defense: require a custom header on state-changing API calls. SameSite cookies alone do
 * not cover all cross-site POST cases.
 *
 * @see <a
 *     href="https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html">OWASP
 *     CSRF</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class CsrfProtectionFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Requested-With";
  public static final String HEADER_VALUE = "XMLHttpRequest";

  private static final Set<String> SAFE_METHODS =
      Set.of(
          HttpMethod.GET.name(),
          HttpMethod.HEAD.name(),
          HttpMethod.OPTIONS.name(),
          HttpMethod.TRACE.name());

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String method = request.getMethod();
    if (method != null && SAFE_METHODS.contains(method)) {
      return true;
    }
    String path = request.getRequestURI();
    return path == null || !path.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER_NAME);
    if (HEADER_VALUE.equals(header)) {
      filterChain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    byte[] body =
        """
                {"message":"Missing CSRF protection header","code":"CSRF_REJECTED"}
                """
            .strip()
            .getBytes(StandardCharsets.UTF_8);
    response.getOutputStream().write(body);
  }
}
