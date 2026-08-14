package com.ai.account.domain.repository;

import com.ai.account.domain.model.AccountUser;
import java.util.Optional;

/** Documentation. */
public interface AccountUserRepository {
  /** Documentation. */
  AccountUser save(AccountUser user);

  /** Documentation. */
  Optional<AccountUser> findByProviderAndSubject(String provider, String subject);

  /** Documentation. */
  Optional<AccountUser> findByLinkedClientId(String linkedClientId);
}
