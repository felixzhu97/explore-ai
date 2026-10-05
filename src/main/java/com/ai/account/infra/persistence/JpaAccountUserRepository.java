package com.ai.account.infra.persistence;

import com.ai.account.domain.model.AccountUser;
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
  @Transactional
  public AccountUser save(AccountUser user) {
    return delegate.saveAndFlush(user);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AccountUser> findByProviderAndSubject(String provider, String subject) {
    if (provider == null || provider.isBlank() || subject == null || subject.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        entityManager.find(
            AccountUser.class, Map.of("provider", provider, "subject", subject), KeyType.NATURAL));
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
