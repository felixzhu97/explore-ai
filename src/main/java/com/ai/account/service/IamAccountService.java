package com.ai.account.service;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.domain.vo.ContactEmail;
import com.ai.account.domain.vo.ExternalIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds or creates the account behind an Explore IAM access token. */
@Service
@RequiredArgsConstructor
public class IamAccountService {

  private final AccountUserRepository accountUserRepository;

  /** Returns the account for the token subject, creating it on first use. */
  @Transactional
  public AccountUser signIn(Jwt jwt) {
    ExternalIdentity identity = ExternalIdentity.iam(jwt.getSubject());
    ContactEmail email = ContactEmail.ofNullable(jwt.getClaimAsString("email"));
    return accountUserRepository
        .findByIdentity(identity)
        .map(
            user -> {
              user.recordSignIn(email, null);
              return accountUserRepository.save(user);
            })
        .orElseGet(() -> accountUserRepository.save(AccountUser.create(identity, email, null)));
  }
}
