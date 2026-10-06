package com.ai.account.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.vo.ClientId;
import com.ai.account.domain.vo.ContactEmail;
import com.ai.account.domain.vo.ExternalIdentity;
import com.ai.testsupport.AbstractDataJpaTest;
import com.ai.testsupport.JpaTestPackages;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.ai.account.domain", JpaTestPackages.COMMON})
@EnableJpaRepositories(basePackageClasses = SpringDataAccountUserRepository.class)
@Import(JpaAccountUserRepository.class)
class AccountUserJpaTest extends AbstractDataJpaTest {

  private static final String LINKED_CLIENT_ID = "55555555-5555-5555-5555-555555555555";

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataAccountUserRepository repository;
  @Autowired private JpaAccountUserRepository adapter;

  @Test
  @DisplayName("should persist and reload account user when round tripping")
  void shouldPersistAndReloadAccountUserWhenRoundTripping() {
    AccountUser user = linked("google", "subject-123", "user@example.com");

    repository.saveAndFlush(user);
    em.clear();

    AccountUser reloaded = repository.findById(user.getId()).orElseThrow();

    assertThat(reloaded.getProvider()).isEqualTo("google");
    assertThat(reloaded.getSubject()).isEqualTo("subject-123");
    assertThat(reloaded.getEmail()).isEqualTo(new ContactEmail("user@example.com"));
    assertThat(reloaded.getDisplayName()).isEqualTo("octo");
    assertThat(reloaded.getLinkedClientId()).isEqualTo(ClientId.parse(LINKED_CLIENT_ID));
  }

  @Test
  @DisplayName("should find user by natural id when oauth identity lookup")
  void shouldFindUserByNaturalIdWhenOauthIdentityLookup() {
    AccountUser user = linked("github", "gh-42", "dev@example.com");
    repository.saveAndFlush(user);
    em.clear();

    Optional<AccountUser> found = adapter.findByIdentity(ExternalIdentity.of(" GitHub ", "gh-42"));

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo(new ContactEmail("dev@example.com"));
    assertThat(adapter.findByIdentity(ExternalIdentity.of("github", "gh-43"))).isEmpty();
  }

  @Test
  @DisplayName("should find user by linked client id when session link lookup")
  void shouldFindUserByLinkedClientIdWhenSessionLinkLookup() {
    AccountUser user = linked("google", "subject-linked", "linked@example.com");
    repository.saveAndFlush(user);
    em.clear();

    Optional<AccountUser> found = adapter.findByLinkedClientId(ClientId.parse(LINKED_CLIENT_ID));

    assertThat(found).isPresent();
    assertThat(found.get().getSubject()).isEqualTo("subject-linked");
  }

  @Test
  @DisplayName("should clear linked client id when browser unlinked")
  void shouldClearLinkedClientIdWhenBrowserUnlinked() {
    AccountUser user = linked("google", "subject-unlink", "unlink@example.com");
    repository.saveAndFlush(user);
    em.clear();

    AccountUser managed = repository.findById(user.getId()).orElseThrow();
    managed.unlinkBrowser();
    repository.saveAndFlush(managed);
    em.clear();

    AccountUser reloaded = repository.findById(user.getId()).orElseThrow();

    assertThat(reloaded.getLinkedClientId()).isNull();
  }

  private static AccountUser linked(String provider, String subject, String email) {
    AccountUser user =
        AccountUser.create(ExternalIdentity.of(provider, subject), new ContactEmail(email), "octo");
    user.linkBrowser(ClientId.parse(LINKED_CLIENT_ID), null, null);
    return user;
  }
}
