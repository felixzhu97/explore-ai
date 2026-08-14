package com.ai.chat.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcChatSessionMetadataRepository")
class JdbcChatSessionMetadataRepositoryTest {

  private static final String CLIENT_ID = "11111111-1111-1111-1111-111111111111";

  @Mock private JdbcTemplate jdbcTemplate;

  private JdbcChatSessionMetadataRepository repository;

  @BeforeEach
  void setUp() {
    repository = new JdbcChatSessionMetadataRepository(jdbcTemplate);
  }

  @Test
  @DisplayName("should find session by id")
  void shouldFindSessionById() {
    ChatSessionId id = ChatSessionId.generate();
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id.value())))
        .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, CLIENT_ID)));

    Optional<ChatSession> found = repository.findById(id);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(id);
    assertThat(found.get().getClientId()).isEqualTo(CLIENT_ID);
  }

  @Test
  @DisplayName("should return empty when session not found")
  void shouldReturnEmptyWhenSessionNotFound() {
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

    assertThat(repository.findById(ChatSessionId.generate())).isEmpty();
  }

  @Test
  @DisplayName("should find session by id and client id")
  void shouldFindSessionByIdAndClientId() {
    ChatSessionId id = ChatSessionId.generate();
    when(jdbcTemplate.query(
            startsWith("SELECT id"), any(RowMapper.class), eq(id.value()), eq(CLIENT_ID)))
        .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, CLIENT_ID)));

    Optional<ChatSession> found = repository.findByIdAndClientId(id, CLIENT_ID);

    assertThat(found).isPresent();
    assertThat(found.get().belongsTo(CLIENT_ID)).isTrue();
  }

  @Test
  @DisplayName("should reconstitute orphan when client id blank")
  void shouldReconstituteOrphanWhenClientIdBlank() {
    ChatSessionId id = ChatSessionId.generate();
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
        .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, "   ")));

    Optional<ChatSession> found = repository.findById(id);

    assertThat(found).isPresent();
    assertThat(found.get().getClientId()).isEqualTo("__orphan__");
  }

  @Test
  @DisplayName("should merge session metadata on save")
  void shouldMergeSessionMetadataOnSave() {
    ChatSession session = ChatSession.create("My chat", CLIENT_ID);

    repository.save(session);

    verify(jdbcTemplate)
        .update(
            startsWith("MERGE INTO chat_sessions"),
            eq(session.getId().value()),
            eq("My chat"),
            eq(Timestamp.from(session.getCreatedAt())),
            eq(Timestamp.from(session.getLastActivityAt())),
            eq(CLIENT_ID));
  }

  @Test
  @DisplayName("should delete session by id")
  void shouldDeleteSessionById() {
    ChatSessionId id = ChatSessionId.generate();

    repository.delete(id);

    verify(jdbcTemplate).update("DELETE FROM chat_sessions WHERE id = ?", id.value().toString());
  }

  @Test
  @DisplayName("should list sessions by client id")
  void shouldListSessionsByClientId() {
    ChatSessionId id = ChatSessionId.generate();
    when(jdbcTemplate.query(startsWith("SELECT id"), any(RowMapper.class), eq(CLIENT_ID)))
        .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, CLIENT_ID)));

    List<ChatSession> sessions = repository.findByClientId(CLIENT_ID);

    assertThat(sessions).hasSize(1);
    assertThat(sessions.getFirst().getId()).isEqualTo(id);
  }

  @Test
  @DisplayName("should report exists when count positive")
  void shouldReportExistsWhenCountPositive() {
    ChatSessionId id = ChatSessionId.generate();
    when(jdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM chat_sessions WHERE id = ?"),
            eq(Integer.class),
            eq(id.value())))
        .thenReturn(1);

    assertThat(repository.exists(id)).isTrue();
    verify(jdbcTemplate)
        .queryForObject(
            eq("SELECT COUNT(*) FROM chat_sessions WHERE id = ?"),
            eq(Integer.class),
            eq(id.value()));
  }

  @Test
  @DisplayName("should report not exists when count null or zero")
  void shouldReportNotExistsWhenCountNullOrZero() {
    ChatSessionId id = ChatSessionId.generate();
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(null);

    assertThat(repository.exists(id)).isFalse();
  }

  private static ChatSession mapSession(
      RowMapper<ChatSession> mapper, ChatSessionId id, String clientId) throws Exception {
    ResultSet rs = mock(ResultSet.class);
    Instant now = Instant.parse("2026-07-26T10:00:00Z");
    when(rs.getString("id")).thenReturn(id.value());
    when(rs.getString("title")).thenReturn("Title");
    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));
    when(rs.getTimestamp("last_activity_at")).thenReturn(Timestamp.from(now));
    when(rs.getString("owner_key")).thenReturn(clientId);
    return mapper.mapRow(rs, 0);
  }
}
