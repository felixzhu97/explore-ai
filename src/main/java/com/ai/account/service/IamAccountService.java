package com.ai.account.service;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds or creates the account behind an Explore IAM access token. */
@Service
@RequiredArgsConstructor
public class IamAccountService {

  private final AccountRepository accountRepository;

  /** Returns the account for the token subject, creating it on first use. */
  @Transactional
  public Account signIn(Jwt jwt) {
    ExternalIdentity identity = ExternalIdentity.iam(jwt.getSubject());
    ContactEmail email = ContactEmail.ofNullable(jwt.getClaimAsString("email"));
    return accountRepository
        .findByIdentity(identity)
        .map(
            account -> {
              account.recordSignIn(email, null);
              return accountRepository.save(account);
            })
        .orElseGet(() -> accountRepository.save(Account.create(identity, email, null)));
  }
}
