package com.ai.account.service.usecase;

/** Reassigns guest-partition rows to a signed-in account after OAuth login. */
public interface OwnerMergeUseCase {
  void mergeClientIntoAccount(String clientId, String accountUserId);
}
