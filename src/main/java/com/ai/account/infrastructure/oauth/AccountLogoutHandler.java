package com.ai.account.infrastructure.oauth;

import com.ai.account.application.usecase.AccountUseCase;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** Clears OAuth ↔ Client Identity link on logout so the UI returns to guest mode. */
@Component
public class AccountLogoutHandler implements LogoutHandler {

    private final AccountUseCase accountUseCase;

    public AccountLogoutHandler(AccountUseCase accountUseCase) {
        this.accountUseCase = accountUseCase;
    }

    @Override
    public void logout(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Object attribute = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
        if (attribute instanceof String clientId && !clientId.isBlank()) {
            accountUseCase.unlinkClient(clientId);
        }
    }
}
