package com.ai.common.web;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Builds Set-Cookie values for anonymous client identity.
 */
@Component
public class ClientIdentityCookieFactory {

    private final ClientIdentityProperties properties;

    public ClientIdentityCookieFactory(ClientIdentityProperties properties) {
        this.properties = properties;
    }

    public String cookieName() {
        return properties.getCookieName();
    }

    public ResponseCookie issue(String clientId) {
        return ResponseCookie.from(properties.getCookieName(), clientId)
                .httpOnly(true)
                .path("/")
                .maxAge(properties.getMaxAge())
                .sameSite(properties.getSameSite())
                .secure(properties.isSecure())
                .build();
    }

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
