package com.ai.account.application.usecase;

import com.ai.account.web.dto.AccountMeResponse;

public interface AccountUseCase {

    AccountMeResponse currentAccount(String clientId);

    void linkOAuthUser(String provider, String subject, String email, String clientId);

    /** Clears OAuth ↔ Client Identity link so the browser returns to guest mode. */
    void unlinkClient(String clientId);

    boolean isLoginAvailable();
}
