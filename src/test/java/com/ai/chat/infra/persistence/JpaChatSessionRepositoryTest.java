package com.ai.chat.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaChatSessionRepository")
class JpaChatSessionRepositoryTest {

  private static final String OWNER_KEY = "c:11111111-1111-1111-1111-111111111111";

  @Mock private SpringDataChatSessionRepository delegate;

  private JpaChatSessionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new JpaChatSessionRepository(delegate);
  }

  @Test
  @DisplayName("should find session by id")
  void shouldFindSessionById() {
    ChatSessionId id = ChatSessionId.generate();
    ChatSession session = ChatSession.create("My chat", OWNER_KEY);
    org.mockito.Mockito.when(delegate.findById(id)).thenReturn(Optional.of(session));

    Optional<ChatSession> found = repository.findById(id);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(session.getId());
    assertThat(found.get().getClientId()).isEqualTo(OWNER_KEY);
  }

  @Test
  @DisplayName("should return empty when session not found")
  void shouldReturnEmptyWhenSessionNotFound() {
    ChatSessionId id = ChatSessionId.generate();
    org.mockito.Mockito.when(delegate.findById(id)).thenReturn(Optional.empty());

    assertThat(repository.findById(id)).isEmpty();
  }

  @Test
  @DisplayName("should find session by id and client id")
  void shouldFindSessionByIdAndClientId() {
    ChatSessionId id = ChatSessionId.generate();
    ChatSession session = ChatSession.create("Owned", OWNER_KEY);
    org.mockito.Mockito.when(delegate.findByIdAndOwnerKeyValue(id.value(), OWNER_KEY))
        .thenReturn(Optional.of(session));

    Optional<ChatSession> found = repository.findByIdAndClientId(id, OWNER_KEY);

    assertThat(found).isPresent();
    assertThat(found.get().belongsTo(OWNER_KEY)).isTrue();
  }

  @Test
  @DisplayName("should map bare client id to owner key on lookup")
  void shouldMapBareClientIdToOwnerKeyOnLookup() {
    ChatSessionId id = ChatSessionId.generate();
    org.mockito.Mockito.when(
            delegate.findByIdAndOwnerKeyValue(id.value(), "c:11111111-1111-1111-1111-111111111111"))
        .thenReturn(Optional.empty());

    repository.findByIdAndClientId(id, "11111111-1111-1111-1111-111111111111");

    org.mockito.Mockito.verify(delegate)
        .findByIdAndOwnerKeyValue(id.value(), "c:11111111-1111-1111-1111-111111111111");
  }

  @Test
  @DisplayName("should persist session metadata on save")
  void shouldPersistSessionMetadataOnSave() {
    ChatSession session = ChatSession.create("My chat", OWNER_KEY);
    org.mockito.Mockito.when(delegate.saveAndFlush(session)).thenReturn(session);

    repository.save(session);

    org.mockito.Mockito.verify(delegate).saveAndFlush(session);
  }

  @Test
  @DisplayName("should delete session by id")
  void shouldDeleteSessionById() {
    ChatSessionId id = ChatSessionId.generate();

    repository.delete(id);

    org.mockito.Mockito.verify(delegate).deleteById(id);
  }

  @Test
  @DisplayName("should list sessions by client id")
  void shouldListSessionsByClientId() {
    ChatSession session = ChatSession.create("Listed", OWNER_KEY);
    org.mockito.Mockito.when(delegate.findByOwnerKeyValueOrderByUpdatedAtDesc(OWNER_KEY))
        .thenReturn(List.of(session));

    List<ChatSession> sessions = repository.findByClientId(OWNER_KEY);

    assertThat(sessions).hasSize(1);
    assertThat(sessions.getFirst().getId()).isEqualTo(session.getId());
  }

  @Test
  @DisplayName("should report exists when count positive")
  void shouldReportExistsWhenCountPositive() {
    ChatSessionId id = ChatSessionId.generate();
    org.mockito.Mockito.when(delegate.existsById(id)).thenReturn(true);

    assertThat(repository.exists(id)).isTrue();
  }

  @Test
  @DisplayName("should find inactive sessions since cutoff")
  void shouldFindInactiveSessionsSinceCutoff() {
    Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
    ChatSession inactive = ChatSession.create("Old", OWNER_KEY);
    org.mockito.Mockito.when(delegate.findByUpdatedAtBeforeOrderByUpdatedAtAsc(cutoff))
        .thenReturn(List.of(inactive));

    List<ChatSession> result = repository.findInactiveSince(cutoff);

    assertThat(result).containsExactly(inactive);
  }

  @Test
  @DisplayName("should normalize owner key when listing by bare client id")
  void shouldNormalizeOwnerKeyWhenListingByBareClientId() {
    ArgumentCaptor<String> ownerKeyCaptor = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.when(
            delegate.findByOwnerKeyValueOrderByUpdatedAtDesc(ownerKeyCaptor.capture()))
        .thenReturn(List.of());

    repository.findByClientId("11111111-1111-1111-1111-111111111111");

    assertThat(ownerKeyCaptor.getValue()).isEqualTo("c:11111111-1111-1111-1111-111111111111");
  }
}
