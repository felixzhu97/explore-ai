package com.ai.account.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.account.domain.model.AccountUser;
import com.ai.testsupport.AbstractDataJpaTest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.ai.account.domain", "com.ai.base.domain", "com.ai.common.domain"})
@EnableJpaRepositories(basePackageClasses = SpringDataAccountUserRepository.class)
class AccountUserJpaTest extends AbstractDataJpaTest {

  private static final String LINKED_CLIENT_ID = "55555555-5555-5555-5555-555555555555";

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataAccountUserRepository repository;

  @Test
  @DisplayName("should persist and reload account user when round tripping")
  void shouldPersistAndReloadAccountUserWhenRoundTripping() {
    AccountUser user =
        AccountUser.create("google", "subject-123", "user@example.com", LINKED_CLIENT_ID);

    repository.saveAndFlush(user);
    em.clear();

    AccountUser reloaded = repository.findById(user.getId()).orElseThrow();

    assertThat(reloaded.getProvider()).isEqualTo("google");
    assertThat(reloaded.getSubject()).isEqualTo("subject-123");
    assertThat(reloaded.getEmail()).isEqualTo("user@example.com");
    assertThat(reloaded.getLinkedClientId()).isEqualTo(LINKED_CLIENT_ID);
  }

  @Test
  @DisplayName("should find user by provider and subject when oauth identity lookup")
  void shouldFindUserByProviderAndSubjectWhenOauthIdentityLookup() {
    AccountUser user = AccountUser.create("github", "gh-42", "dev@example.com", LINKED_CLIENT_ID);
    repository.saveAndFlush(user);
    em.clear();

    Optional<AccountUser> found = repository.findByProviderAndSubject("github", "gh-42");

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("dev@example.com");
  }

  @Test
  @DisplayName("should find user by linked client id when session link lookup")
  void shouldFindUserByLinkedClientIdWhenSessionLinkLookup() {
    AccountUser user =
        AccountUser.create("google", "subject-linked", "linked@example.com", LINKED_CLIENT_ID);
    repository.saveAndFlush(user);
    em.clear();

    Optional<AccountUser> found = repository.findByLinkedClientId(LINKED_CLIENT_ID);

    assertThat(found).isPresent();
    assertThat(found.get().getSubject()).isEqualTo("subject-linked");
  }

  @Test
  @DisplayName("should clear linked client id when browser unlinked")
  void shouldClearLinkedClientIdWhenBrowserUnlinked() {
    AccountUser user =
        AccountUser.create("google", "subject-unlink", "unlink@example.com", LINKED_CLIENT_ID);
    repository.saveAndFlush(user);
    em.clear();

    AccountUser managed = repository.findById(user.getId()).orElseThrow();
    managed.unlinkBrowser();
    repository.saveAndFlush(managed);
    em.clear();

    AccountUser reloaded = repository.findById(user.getId()).orElseThrow();

    assertThat(reloaded.getLinkedClientId()).isNull();
  }
}
