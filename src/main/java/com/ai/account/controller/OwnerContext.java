package com.ai.account.controller;

import com.ai.account.service.CurrentOwnerResolver;
import com.ai.common.controller.ClientIdentity;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Web helper: cookie Client Identity and/or IAM JWT → data {@link OwnerKey}. */
@Component
public class OwnerContext {

  private final CurrentOwnerResolver currentOwnerResolver;

  public OwnerContext(CurrentOwnerResolver currentOwnerResolver) {
    this.currentOwnerResolver = currentOwnerResolver;
  }

  public OwnerKey require(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return currentOwnerResolver.resolveFromJwt(jwtAuth.getToken());
    }
    String clientId = ClientIdentity.require(request);
    return currentOwnerResolver.resolve(clientId, authentication);
  }

  public String requireValue(HttpServletRequest request) {
    return require(request).value();
  }
}
