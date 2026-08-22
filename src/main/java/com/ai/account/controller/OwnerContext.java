package com.ai.account.controller;

import com.ai.account.service.CurrentOwnerResolver;
import com.ai.common.controller.ClientIdentity;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Web helper: cookie Client Identity + security context → data {@link OwnerKey}. */
@Component
public class OwnerContext {

  private final CurrentOwnerResolver currentOwnerResolver;

  /** Documentation. */
  public OwnerContext(CurrentOwnerResolver currentOwnerResolver) {
    this.currentOwnerResolver = currentOwnerResolver;
  }

  /** Documentation. */
  public OwnerKey require(HttpServletRequest request) {
    String clientId = ClientIdentity.require(request);
    return currentOwnerResolver.resolve(
        clientId, SecurityContextHolder.getContext().getAuthentication());
  }

  /** Documentation. */
  public String requireValue(HttpServletRequest request) {
    return require(request).value();
  }
}
