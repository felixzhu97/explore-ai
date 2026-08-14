package com.ai.account.application.usecase;

/** Reassigns guest-partition rows to a signed-in account after OAuth login. */
public interface OwnerMergeUseCase {
  /** Documentation. */
  void mergeClientIntoAccount(String clientId, String accountUserId);
}
