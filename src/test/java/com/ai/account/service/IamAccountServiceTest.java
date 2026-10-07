package com.ai.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
@DisplayName("IamAccountService")
class IamAccountServiceTest {

  @Mock private AccountRepository accountRepository;

  @InjectMocks private IamAccountService service;

  @Test
  @DisplayName("should create the account when the token subject is new")
  void shouldCreateTheAccountWhenTheTokenSubjectIsNew() {
    when(accountRepository.findByIdentity(ExternalIdentity.iam("iam-new")))
        .thenReturn(Optional.empty());
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Account account = service.signIn(jwt("iam-new", "new@example.com"));

    assertThat(account.identity()).isEqualTo(ExternalIdentity.iam("iam-new"));
    assertThat(account.getEmail()).isEqualTo(new ContactEmail("new@example.com"));
  }

  @Test
  @DisplayName("should refresh the email when an existing account signs in again")
  void shouldRefreshTheEmailWhenAnExistingAccountSignsInAgain() {
    Account existing =
        Account.create(ExternalIdentity.iam("iam-sub"), new ContactEmail("old@example.com"), null);
    when(accountRepository.findByIdentity(ExternalIdentity.iam("iam-sub")))
        .thenReturn(Optional.of(existing));
    when(accountRepository.save(existing)).thenReturn(existing);

    Account account = service.signIn(jwt("iam-sub", "new@example.com"));

    assertThat(account).isSameAs(existing);
    assertThat(account.getEmail()).isEqualTo(new ContactEmail("new@example.com"));
  }

  @Test
  @DisplayName("should reject the token when its subject is missing")
  void shouldRejectTheTokenWhenItsSubjectIsMissing() {
    Jwt jwt =
        Jwt.withTokenValue("t")
            .header("alg", "none")
            .claim("email", "x@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

    assertThatThrownBy(() -> service.signIn(jwt)).isInstanceOf(IllegalArgumentException.class);
  }

  static Jwt jwt(String subject, String email) {
    return Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject(subject)
        .claim("email", email)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }
}
