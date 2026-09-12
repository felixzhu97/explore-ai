package com.ai.account.infra.config;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Requires a JWT {@code SCOPE_*} authority only when the caller presents a Bearer JWT; anonymous
 * Client Identity traffic remains permitted.
 */
final class JwtPresentScopeAuthorization {

  private JwtPresentScopeAuthorization() {}

  static AuthorizationManager<RequestAuthorizationContext> requireScope(String scope) {
    String authority = "SCOPE_" + scope;
    return (authentication, context) -> {
      Authentication auth = authentication.get();
      if (!(auth instanceof JwtAuthenticationToken)) {
        return new AuthorizationDecision(true);
      }
      boolean granted =
          auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
      return new AuthorizationDecision(granted);
    };
  }
}
