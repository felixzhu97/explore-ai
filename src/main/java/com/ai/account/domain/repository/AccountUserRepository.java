package com.ai.account.domain.repository;

import com.ai.account.domain.model.AccountUser;
import java.util.Optional;

/** Repository of account users looked up by OAuth identity or linked client id. */
public interface AccountUserRepository {
  /** Finds the account for an OAuth provider and subject. */
  Optional<AccountUser> findByProviderAndSubject(String provider, String subject);

  /** Finds the account linked to a browser Client Identity. */
  Optional<AccountUser> findByLinkedClientId(String linkedClientId);

  /** Saves the account and returns the stored copy. */
  AccountUser save(AccountUser user);
}
