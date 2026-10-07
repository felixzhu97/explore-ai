package com.ai.account.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.testsupport.AbstractDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccountUserRepositoryTest extends AbstractDataJpaTest {

  private static final ClientId LINKED_CLIENT =
      ClientId.parse("55555555-5555-5555-5555-555555555555");

  @Autowired private AccountUserRepository repository;

  @Test
  @DisplayName("should find the account by its normalized sign-in identity")
  void shouldFindTheAccountByItsNormalizedSignInIdentity() {
    save("github", "gh-42", "dev@example.com");
    flushAndClear();

    AccountUser found =
        repository.findByIdentity(ExternalIdentity.of(" GitHub ", "gh-42")).orElseThrow();

    assertThat(found.getEmail()).isEqualTo(new ContactEmail("dev@example.com"));
    assertThat(found.getDisplayName()).isEqualTo("octo");
    assertThat(repository.findByIdentity(ExternalIdentity.of("github", "gh-43"))).isEmpty();
  }

  @Test
  @DisplayName("should stop finding the account by client id once the browser is unlinked")
  void shouldStopFindingTheAccountByClientIdOnceTheBrowserIsUnlinked() {
    save("google", "subject-linked", "linked@example.com");
    flushAndClear();

    AccountUser linked = repository.findByLinkedClientId(LINKED_CLIENT).orElseThrow();
    assertThat(linked.getSubject()).isEqualTo("subject-linked");

    linked.unlinkBrowser();
    repository.save(linked);
    flushAndClear();

    assertThat(repository.findByLinkedClientId(LINKED_CLIENT)).isEmpty();
  }

  private void save(String provider, String subject, String email) {
    AccountUser user =
        AccountUser.create(ExternalIdentity.of(provider, subject), new ContactEmail(email), "octo");
    user.linkBrowser(LINKED_CLIENT, null, null);
    repository.save(user);
  }
}
