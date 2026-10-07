package com.ai.account.infra.persistence;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountUserRepository;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.KeyType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for account_users. */
@Repository
@RequiredArgsConstructor
public class JpaAccountUserRepository implements AccountUserRepository {

  private final SpringDataAccountUserRepository delegate;
  private final EntityManager entityManager;

  @Override
  @Transactional(readOnly = true)
  public Optional<AccountUser> findByIdentity(ExternalIdentity identity) {
    return Optional.ofNullable(
        entityManager.find(
            AccountUser.class,
            Map.of("provider", identity.provider(), "subject", identity.subject()),
            KeyType.NATURAL));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AccountUser> findByLinkedClientId(ClientId linkedClientId) {
    return delegate.findByLinkedClientId(linkedClientId);
  }

  @Override
  @Transactional
  public AccountUser save(AccountUser user) {
    return delegate.saveAndFlush(user);
  }
}
