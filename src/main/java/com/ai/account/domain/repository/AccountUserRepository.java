package com.ai.account.domain.repository;

import com.ai.account.domain.model.AccountUser;
import java.util.Optional;

public interface AccountUserRepository {

    AccountUser save(AccountUser user);

    Optional<AccountUser> findByProviderAndSubject(String provider, String subject);

    Optional<AccountUser> findByLinkedClientId(String linkedClientId);
}
