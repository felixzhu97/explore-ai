package com.ai.account.service;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountRepository;
import com.ai.common.exception.DomainException;
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
    requirePersonToken(jwt);
    ExternalIdentity identity = ExternalIdentity.createIamIdentity(jwt.getSubject());
    ContactEmail email = ContactEmail.parseOptionalEmail(jwt.getClaimAsString("email"));
    return accountRepository
        .findByIdentity(identity)
        .map(
            account -> {
              account.recordSignIn(email, null);
              return accountRepository.save(account);
            })
        .orElseGet(() -> accountRepository.save(Account.createAccount(identity, email, null)));
  }

  /**
   * Refuses {@code client_credentials} tokens: their subject is the client itself, which is also
   * the audience, and a machine must never become an Account.
   */
  private static void requirePersonToken(Jwt jwt) {
    if (jwt.getSubject() != null
        && jwt.getAudience() != null
        && jwt.getAudience().contains(jwt.getSubject())) {
      throw DomainException.createForbiddenError(
          "MACHINE_TOKEN", "Client tokens cannot act as an account");
    }
  }
}
