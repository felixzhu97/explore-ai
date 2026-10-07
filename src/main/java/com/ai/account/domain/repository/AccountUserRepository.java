package com.ai.account.domain.repository;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.model.AccountUserId;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ExternalIdentity;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repository of account users looked up by sign-in identity or linked client id. */
public interface AccountUserRepository extends Repository<AccountUser, AccountUserId> {

  /** Finds the account for a provider and subject. */
  Optional<AccountUser> findByProviderAndSubject(String provider, String subject);

  /** Finds the account for a sign-in identity. */
  default Optional<AccountUser> findByIdentity(ExternalIdentity identity) {
    return findByProviderAndSubject(identity.provider(), identity.subject());
  }

  /** Finds the account linked to a browser Client Identity. */
  Optional<AccountUser> findByLinkedClientId(ClientId linkedClientId);

  /** Saves the account and returns the stored copy. */
  AccountUser save(AccountUser user);
}
