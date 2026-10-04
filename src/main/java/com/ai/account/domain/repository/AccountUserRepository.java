package com.ai.account.domain.repository;

import com.ai.account.domain.model.AccountUser;
import java.util.Optional;

/** Repository of account users looked up by OAuth identity or linked client id. */
public interface AccountUserRepository {
  AccountUser save(AccountUser user);

  Optional<AccountUser> findByProviderAndSubject(String provider, String subject);

  Optional<AccountUser> findByLinkedClientId(String linkedClientId);
}
