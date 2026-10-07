package com.ai.chat.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.model.ChatSessionId;
import com.ai.chat.domain.model.SessionTitle;
import com.ai.common.domain.model.OwnerKey;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatSessionRepositoryTest extends AbstractDataJpaTest {

  private static final OwnerKey OWNER = OwnerKey.parse("c:11111111-1111-1111-1111-111111111111");
  private static final OwnerKey OTHER = OwnerKey.parse("c:22222222-2222-2222-2222-222222222222");

  @Autowired private ChatSessionRepository repository;

  @Test
  @DisplayName("should find the session only for its owner")
  void shouldFindTheSessionOnlyForItsOwner() {
    ChatSession session = repository.save(ChatSession.create("Planning", OWNER.value()));
    flushAndClear();

    ChatSession reloaded = repository.findByIdAndOwnerKey(session.getId(), OWNER).orElseThrow();

    assertThat(reloaded.getTitle()).isEqualTo("Planning");
    assertThat(reloaded.getOwnerKey()).isEqualTo(OWNER);
    assertThat(repository.findByIdAndOwnerKey(session.getId(), OTHER)).isEmpty();
  }

  @Test
  @DisplayName("should list only the owner's sessions most recently active first")
  void shouldListOnlyTheOwnersSessionsMostRecentlyActiveFirst() {
    save("Older", "2026-01-01T00:00:00Z", OWNER);
    save("Newer", "2026-06-01T00:00:00Z", OWNER);
    save("Foreign", "2026-07-01T00:00:00Z", OTHER);
    flushAndClear();

    assertThat(repository.findAllByOwnerKeyOrderByLastActivityAtDesc(OWNER))
        .extracting(ChatSession::getTitle)
        .containsExactly("Newer", "Older");
  }

  @Test
  @DisplayName("should find every owner's sessions inactive since the cutoff oldest first")
  void shouldFindEveryOwnersSessionsInactiveSinceTheCutoffOldestFirst() {
    save("Recent", "2026-06-01T00:00:00Z", OWNER);
    save("Stale", "2026-02-01T00:00:00Z", OTHER);
    save("Stalest", "2026-01-01T00:00:00Z", OWNER);
    flushAndClear();

    assertThat(
            repository.findAllByLastActivityAtBeforeOrderByLastActivityAtAsc(
                Instant.parse("2026-03-01T00:00:00Z")))
        .extracting(ChatSession::getTitle)
        .containsExactly("Stalest", "Stale");
  }

  @Test
  @DisplayName("should keep last activity when a generated title is stored")
  void shouldKeepLastActivityWhenAGeneratedTitleIsStored() {
    Instant lastActivity = Instant.parse("2026-01-01T00:00:00Z");
    ChatSession session =
        repository.save(
            ChatSession.of(ChatSessionId.generate(), null, lastActivity, OWNER.value()));
    em.flush();

    session.applyGeneratedTitle(SessionTitle.generated("Trip plan"));
    repository.save(session);
    flushAndClear();

    ChatSession reloaded = repository.findById(session.getId()).orElseThrow();
    assertThat(reloaded.getTitle()).isEqualTo("Trip plan");
    assertThat(reloaded.getLastActivityAt()).isEqualTo(lastActivity);
  }

  @Test
  @DisplayName("should no longer find a deleted session")
  void shouldNoLongerFindADeletedSession() {
    ChatSession session = repository.save(ChatSession.create("Gone", OWNER.value()));
    flushAndClear();

    repository.deleteById(session.getId());
    flushAndClear();

    assertThat(repository.existsById(session.getId())).isFalse();
  }

  private void save(String title, String lastActivityAt, OwnerKey owner) {
    repository.save(
        ChatSession.of(
            ChatSessionId.generate(), title, Instant.parse(lastActivityAt), owner.value()));
  }
}
