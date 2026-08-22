package com.ai.account.service.usecase;

import com.ai.account.controller.dto.AccountMeResponse;
import java.util.List;

/** Documentation. */
public interface AccountUseCase {
  /** Documentation. */
  AccountMeResponse currentAccount(String clientId);

  /** Links OAuth identity to the browser cookie and returns the account user id. */
  String linkOAuthUser(String provider, String subject, String email, String clientId);

  /** Clears OAuth ↔ Client Identity link so the browser returns to guest mode. */
  void unlinkClient(String clientId);

  /** Documentation. */
  boolean isLoginAvailable();

  /** Registration ids that are currently configured (e.g. {@code google}, {@code github}). */
  List<String> loginProviders();
}
