package com.ai.account.application.usecase;

import com.ai.account.web.dto.AccountMeResponse;
import java.util.List;

public interface AccountUseCase {

    AccountMeResponse currentAccount(String clientId);

    void linkOAuthUser(String provider, String subject, String email, String clientId);

    /** Clears OAuth ↔ Client Identity link so the browser returns to guest mode. */
    void unlinkClient(String clientId);

    boolean isLoginAvailable();

    /** Registration ids that are currently configured (e.g. {@code google}, {@code github}). */
    List<String> loginProviders();
}
