package com.ai.common.controller;

import java.time.Duration;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds Set-Cookie values for anonymous client identity. */
@Component
public class ClientIdentityCookieFactory {

  private final ClientIdentityProperties properties;

  public ClientIdentityCookieFactory(ClientIdentityProperties properties) {
    this.properties = properties;
  }

  public String cookieName() {
    return properties.getCookieName();
  }

  /** Creates an HttpOnly cookie carrying the client id with the configured lifetime and flags. */
  public ResponseCookie issue(String clientId) {
    return ResponseCookie.from(properties.getCookieName(), clientId)
        .httpOnly(true)
        .path("/")
        .maxAge(properties.getMaxAge())
        .sameSite(properties.getSameSite())
        .secure(properties.isSecure())
        .build();
  }

  /** Creates an empty, immediately expiring cookie that removes the client identity. */
  public ResponseCookie clear() {
    return ResponseCookie.from(properties.getCookieName(), "")
        .httpOnly(true)
        .path("/")
        .maxAge(Duration.ZERO)
        .sameSite(properties.getSameSite())
        .secure(properties.isSecure())
        .build();
  }

  public String newClientId() {
    return UUID.randomUUID().toString();
  }
}
