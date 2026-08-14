package com.ai.common.web;

import java.time.Duration;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds Set-Cookie values for anonymous client identity. */
@Component
public class ClientIdentityCookieFactory {

  private final ClientIdentityProperties properties;

  /** Documentation. */
  public ClientIdentityCookieFactory(ClientIdentityProperties properties) {
    this.properties = properties;
  }

  /** Documentation. */
  public String cookieName() {
    return properties.getCookieName();
  }

  /** Documentation. */
  public ResponseCookie issue(String clientId) {
    return ResponseCookie.from(properties.getCookieName(), clientId)
        .httpOnly(true)
        .path("/")
        .maxAge(properties.getMaxAge())
        .sameSite(properties.getSameSite())
        .secure(properties.isSecure())
        .build();
  }

  /** Documentation. */
  public ResponseCookie clear() {
    return ResponseCookie.from(properties.getCookieName(), "")
        .httpOnly(true)
        .path("/")
        .maxAge(Duration.ZERO)
        .sameSite(properties.getSameSite())
        .secure(properties.isSecure())
        .build();
  }

  /** Documentation. */
  public String newClientId() {
    return UUID.randomUUID().toString();
  }
}
