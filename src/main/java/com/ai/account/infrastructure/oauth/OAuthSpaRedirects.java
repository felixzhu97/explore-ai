package com.ai.account.infrastructure.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds SPA return URLs after Google OAuth.
 *
 * <p>Locally the Angular proxy makes the OAuth callback share the SPA origin. An absolute redirect
 * from {@code localhost} → {@code 127.0.0.1} (or the reverse) drops {@code JSESSIONID} because
 * browsers treat those as different cookie hosts.
 */
final class OAuthSpaRedirects {

  private OAuthSpaRedirects() {}

  static String afterLogin(
      HttpServletRequest request, String configuredRedirectUrl, String loginValue) {
    if (shouldUseConfiguredAbsolute(configuredRedirectUrl)) {
      return withLoginParam(configuredRedirectUrl, loginValue);
    }
    // Same host/port as the OAuth callback (e.g. http://localhost:4200/login/oauth2/...).
    return UriComponentsBuilder.newInstance()
        .scheme(request.getScheme())
        .host(request.getServerName())
        .port(request.getServerPort())
        .path("/")
        .queryParam("login", loginValue)
        .build()
        .toUriString();
  }

  static String withLoginParam(String redirectUrl, String loginValue) {
    String base = redirectUrl == null || redirectUrl.isBlank() ? "/" : redirectUrl.trim();
    if (base.startsWith("/")) {
      return UriComponentsBuilder.fromPath(
              base.contains("?") ? base.substring(0, base.indexOf('?')) : base)
          .replaceQuery(base.contains("?") ? base.substring(base.indexOf('?') + 1) : null)
          .replaceQueryParam("login", loginValue)
          .build()
          .toUriString();
    }
    return UriComponentsBuilder.fromUriString(base)
        .replaceQueryParam("login", loginValue)
        .build()
        .toUriString();
  }

  private static boolean shouldUseConfiguredAbsolute(String configuredRedirectUrl) {
    if (configuredRedirectUrl == null || configuredRedirectUrl.isBlank()) {
      return false;
    }
    String trimmed = configuredRedirectUrl.trim();
    if (trimmed.startsWith("/")) {
      return false;
    }
    try {
      URI uri = URI.create(trimmed);
      String host = uri.getHost();
      if (host == null) {
        return false;
      }
      // Loopback absolutes are unsafe across localhost vs 127.0.0.1 cookie jars.
      return !isLoopback(host);
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  private static boolean isLoopback(String host) {
    String h = host.toLowerCase();
    return "localhost".equals(h) || "127.0.0.1".equals(h) || "[::1]".equals(h) || "::1".equals(h);
  }
}
