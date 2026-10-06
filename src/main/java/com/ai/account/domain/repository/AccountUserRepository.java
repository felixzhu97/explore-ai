package com.ai.account.domain.repository;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.vo.ClientId;
import com.ai.account.domain.vo.ExternalIdentity;
import java.util.Optional;

/** Repository of account users looked up by sign-in identity or linked client id. */
public interface AccountUserRepository {
  /** Finds the account for a sign-in identity. */
  Optional<AccountUser> findByIdentity(ExternalIdentity identity);

  /** Finds the account linked to a browser Client Identity. */
  Optional<AccountUser> findByLinkedClientId(ClientId linkedClientId);

  /** Saves the account and returns the stored copy. */
  AccountUser save(AccountUser user);
}
