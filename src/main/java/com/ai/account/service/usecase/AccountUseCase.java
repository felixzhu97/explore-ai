package com.ai.account.service.usecase;

import com.ai.account.controller.dto.AccountMeResponse;
import java.util.List;

/** Account session state, OAuth linking to Client Identity, and available login providers. */
public interface AccountUseCase {
  AccountMeResponse currentAccount(String clientId);

  /** Links OAuth identity to the browser cookie and returns the account user id. */
  String linkOAuthUser(String provider, String subject, String email, String clientId);

  /** Clears OAuth ↔ Client Identity link so the browser returns to guest mode. */
  void unlinkClient(String clientId);

  boolean isLoginAvailable();

  /** Registration ids that are currently configured (e.g. {@code google}, {@code github}). */
  List<String> loginProviders();
}
