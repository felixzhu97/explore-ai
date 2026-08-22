package com.ai.account.infra.persistence;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.vo.AccountUserId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link AccountUser}. */
@Repository
public interface SpringDataAccountUserRepository extends JpaRepository<AccountUser, AccountUserId> {

  /** Documentation. */
  Optional<AccountUser> findByProviderAndSubject(String provider, String subject);

  /** Documentation. */
  Optional<AccountUser> findByLinkedClientId(String linkedClientId);
}
