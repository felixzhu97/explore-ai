package com.ai.account.infra.oauth;

import com.ai.account.service.AccountService;
import com.ai.common.controller.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** Clears OAuth ↔ Client Identity link on logout so the UI returns to guest mode. */
@Component
@RequiredArgsConstructor
public class AccountLogoutHandler implements LogoutHandler {

  private final AccountService accountService;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    Object attribute = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
    if (attribute instanceof String clientId && !clientId.isBlank()) {
      accountService.unlinkClient(clientId);
    }
  }
}
