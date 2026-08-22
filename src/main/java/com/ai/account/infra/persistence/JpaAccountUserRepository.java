package com.ai.account.infra.persistence;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for account_users. */
@Repository
public class JpaAccountUserRepository implements AccountUserRepository {

  private final SpringDataAccountUserRepository delegate;

  /** Documentation. */
  public JpaAccountUserRepository(SpringDataAccountUserRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public AccountUser save(AccountUser user) {
    return delegate.saveAndFlush(user);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AccountUser> findByProviderAndSubject(String provider, String subject) {
    return delegate.findByProviderAndSubject(provider, subject);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AccountUser> findByLinkedClientId(String linkedClientId) {
    if (linkedClientId == null || linkedClientId.isBlank()) {
      return Optional.empty();
    }
    return delegate.findByLinkedClientId(linkedClientId);
  }
}
