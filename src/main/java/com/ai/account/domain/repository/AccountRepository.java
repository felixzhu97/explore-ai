package com.ai.account.domain.repository;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.AccountId;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ExternalIdentity;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repository of accounts looked up by sign-in identity or linked client id. */
public interface AccountRepository extends Repository<Account, AccountId> {

  /** Finds the account for a provider and subject. */
  Optional<Account> findByProviderAndSubject(String provider, String subject);

  /** Finds the account for a sign-in identity. */
  default Optional<Account> findByIdentity(ExternalIdentity identity) {
    return findByProviderAndSubject(identity.getProvider(), identity.getSubject());
  }

  /** Finds the account linked to a browser Client Identity. */
  Optional<Account> findByLinkedClientId(ClientId linkedClientId);

  /** Saves the account and returns the stored copy. */
  Account save(Account account);
}
