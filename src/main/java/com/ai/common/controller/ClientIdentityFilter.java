package com.ai.common.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Issues and resolves an anonymous browser client id via HttpOnly cookie, or accepts a trusted BFF
 * identity via {@code X-Service-Key} + {@code X-Client-Id}.
 *
 * @see <a
 *     href="https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html">OWASP
 *     Session Management</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@EnableConfigurationProperties({ClientIdentityProperties.class, ServiceAuthProperties.class})
public class ClientIdentityFilter extends OncePerRequestFilter {

  public static final String SERVICE_KEY_HEADER = "X-Service-Key";
  public static final String CLIENT_ID_HEADER = "X-Client-Id";

  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private final ClientIdentityCookieFactory cookieFactory;
  private final ServiceAuthProperties serviceAuthProperties;

  /** Documentation. */
  public ClientIdentityFilter(
      ClientIdentityCookieFactory cookieFactory, ServiceAuthProperties serviceAuthProperties) {
    this.cookieFactory = cookieFactory;
    this.serviceAuthProperties = serviceAuthProperties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) {
      return true;
    }
    // Include OAuth callback paths so login can link the browser Client Identity cookie.
    return !path.startsWith("/api/")
        && !path.startsWith("/login/oauth2/")
        && !path.startsWith("/oauth2/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String serviceClientId = resolveServiceClientId(request);
    if (serviceClientId != null) {
      request.setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, serviceClientId);
      filterChain.doFilter(request, response);
      return;
    }

    String existing = readCookie(request);
    String clientId;
    if (existing != null) {
      clientId = existing;
    } else {
      clientId = cookieFactory.newClientId();
      response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issue(clientId).toString());
    }
    request.setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, clientId);
    filterChain.doFilter(request, response);
  }

  private String resolveServiceClientId(HttpServletRequest request) {
    if (!serviceAuthProperties.isEnabled()) {
      return null;
    }
    String serviceKey = request.getHeader(SERVICE_KEY_HEADER);
    if (!constantTimeEquals(serviceKey, serviceAuthProperties.getApiKey())) {
      return null;
    }
    String clientId = request.getHeader(CLIENT_ID_HEADER);
    if (clientId == null || !UUID_PATTERN.matcher(clientId).matches()) {
      return null;
    }
    return clientId;
  }

  private String readCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    String cookieName = cookieFactory.cookieName();
    for (Cookie cookie : cookies) {
      if (cookieName.equals(cookie.getName())) {
        String value = cookie.getValue();
        if (value != null && UUID_PATTERN.matcher(value).matches()) {
          return value;
        }
        return null;
      }
    }
    return null;
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    return MessageDigest.isEqual(
        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}
