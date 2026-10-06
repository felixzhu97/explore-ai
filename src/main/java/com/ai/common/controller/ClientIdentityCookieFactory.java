package com.ai.common.controller;

import com.ai.common.infra.config.ClientIdentityProperties;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds Set-Cookie values for anonymous client identity. */
@Component
@RequiredArgsConstructor
public class ClientIdentityCookieFactory {

  private final ClientIdentityProperties properties;

  /** Returns the name of the Client Identity cookie. */
  public String getCookieName() {
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

  /** Creates a new random client id. */
  public String generateClientId() {
    return UUID.randomUUID().toString();
  }
}
