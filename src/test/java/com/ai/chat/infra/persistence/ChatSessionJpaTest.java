package com.ai.chat.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.ai.chat.domain", "com.ai.base.domain", "com.ai.common.domain"})
@EnableJpaRepositories(basePackageClasses = SpringDataChatSessionRepository.class)
@Import(JpaChatSessionRepository.class)
class ChatSessionJpaTest extends AbstractDataJpaTest {

  private static final String BARE_CLIENT_ID = "11111111-1111-1111-1111-111111111111";
  private static final String OWNER_KEY = "c:" + BARE_CLIENT_ID;

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataChatSessionRepository springDataRepository;
  @Autowired private JpaChatSessionRepository jpaRepository;

  @Test
  @DisplayName("should persist and reload chat session when round tripping")
  void shouldPersistAndReloadChatSessionWhenRoundTripping() {
    ChatSession session = ChatSession.create("Planning", OWNER_KEY);

    springDataRepository.saveAndFlush(session);
    em.clear();

    Optional<ChatSession> reloaded = springDataRepository.findById(session.getId());

    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getTitle()).isEqualTo("Planning");
    assertThat(reloaded.get().getClientId()).isEqualTo(OWNER_KEY);
  }

  @Test
  @DisplayName("should store owner key via converter when persisting session")
  void shouldStoreOwnerKeyViaConverterWhenPersistingSession() {
    ChatSession session = ChatSession.create("Owned", OWNER_KEY);

    springDataRepository.saveAndFlush(session);
    em.clear();

    String rawOwnerKey =
        (String)
            em.getEntityManager()
                .createNativeQuery("SELECT owner_key FROM chat_sessions WHERE id = ?")
                .setParameter(1, session.getId().value())
                .getSingleResult();

    assertThat(rawOwnerKey).isEqualTo(OWNER_KEY);
  }

  @Test
  @DisplayName("should return sessions ordered by last activity descending when listing by owner")
  void shouldReturnSessionsOrderedByLastActivityDescendingWhenListingByOwner() {
    Instant older = Instant.parse("2026-01-01T00:00:00Z");
    Instant newer = Instant.parse("2026-06-01T00:00:00Z");
    ChatSession olderSession =
        ChatSession.reconstitute(
            ChatSessionId.generate(), "Older", older, older, List.of(), OWNER_KEY);
    ChatSession newerSession =
        ChatSession.reconstitute(
            ChatSessionId.generate(), "Newer", newer, newer, List.of(), OWNER_KEY);
    springDataRepository.saveAndFlush(olderSession);
    springDataRepository.saveAndFlush(newerSession);
    em.clear();

    List<ChatSession> sessions =
        springDataRepository.findByOwnerKeyValueOrderByUpdatedAtDesc(OWNER_KEY);

    assertThat(sessions).extracting(ChatSession::getTitle).containsExactly("Newer", "Older");
  }

  @Test
  @DisplayName("should find session by id and bare client id when using jpa adapter")
  void shouldFindSessionByIdAndBareClientIdWhenUsingJpaAdapter() {
    ChatSession session = ChatSession.create("Scoped", OWNER_KEY);
    springDataRepository.saveAndFlush(session);
    em.clear();

    Optional<ChatSession> found =
        jpaRepository.findByIdAndClientId(session.getId(), BARE_CLIENT_ID);

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("Scoped");
  }

  @Test
  @DisplayName("should list sessions by bare client id when using jpa adapter")
  void shouldListSessionsByBareClientIdWhenUsingJpaAdapter() {
    ChatSession session = ChatSession.create("Listed", OWNER_KEY);
    springDataRepository.saveAndFlush(session);
    em.clear();

    List<ChatSession> sessions = jpaRepository.findByClientId(BARE_CLIENT_ID);

    assertThat(sessions).hasSize(1);
    assertThat(sessions.getFirst().getId()).isEqualTo(session.getId());
  }
}
