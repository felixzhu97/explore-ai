package com.ai.chat.infrastructure;

import com.ai.chat.domain.ChatSession;
import com.ai.chat.domain.ChatSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcChatSessionMetadataRepository")
class JdbcChatSessionMetadataRepositoryTest {

    private static final String CLIENT_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcChatSessionMetadataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcChatSessionMetadataRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("should_find_session_by_id")
    void should_find_session_by_id() {
        ChatSessionId id = ChatSessionId.generate();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id.value())))
                .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, CLIENT_ID)));

        Optional<ChatSession> found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
        assertThat(found.get().getClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    @DisplayName("should_return_empty_when_session_not_found")
    void should_return_empty_when_session_not_found() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        assertThat(repository.findById(ChatSessionId.generate())).isEmpty();
    }

    @Test
    @DisplayName("should_find_session_by_id_and_client_id")
    void should_find_session_by_id_and_client_id() {
        ChatSessionId id = ChatSessionId.generate();
        when(jdbcTemplate.query(startsWith("SELECT id"), any(RowMapper.class), eq(id.value()), eq(CLIENT_ID)))
                .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, CLIENT_ID)));

        Optional<ChatSession> found = repository.findByIdAndClientId(id, CLIENT_ID);

        assertThat(found).isPresent();
        assertThat(found.get().belongsTo(CLIENT_ID)).isTrue();
    }

    @Test
    @DisplayName("should_reconstitute_orphan_when_client_id_blank")
    void should_reconstitute_orphan_when_client_id_blank() {
        ChatSessionId id = ChatSessionId.generate();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, "   ")));

        Optional<ChatSession> found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getClientId()).isEqualTo("__orphan__");
    }

    @Test
    @DisplayName("should_merge_session_metadata_on_save")
    void should_merge_session_metadata_on_save() {
        ChatSession session = ChatSession.create("My chat", CLIENT_ID);

        repository.save(session);

        verify(jdbcTemplate).update(
                startsWith("MERGE INTO chat_sessions"),
                eq(session.getId().value()),
                eq("My chat"),
                eq(Timestamp.from(session.getCreatedAt())),
                eq(Timestamp.from(session.getLastActivityAt())),
                eq(CLIENT_ID));
    }

    @Test
    @DisplayName("should_delete_session_by_id")
    void should_delete_session_by_id() {
        ChatSessionId id = ChatSessionId.generate();

        repository.delete(id);

        verify(jdbcTemplate).update("DELETE FROM chat_sessions WHERE id = ?", id.value().toString());
    }

    @Test
    @DisplayName("should_list_sessions_by_client_id")
    void should_list_sessions_by_client_id() {
        ChatSessionId id = ChatSessionId.generate();
        when(jdbcTemplate.query(startsWith("SELECT id"), any(RowMapper.class), eq(CLIENT_ID)))
                .thenAnswer(invocation -> List.of(mapSession(invocation.getArgument(1), id, CLIENT_ID)));

        List<ChatSession> sessions = repository.findByClientId(CLIENT_ID);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.getFirst().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("should_report_exists_when_count_positive")
    void should_report_exists_when_count_positive() {
        ChatSessionId id = ChatSessionId.generate();
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM chat_sessions WHERE id = ?"),
                eq(Integer.class),
                eq(id.value())))
                .thenReturn(1);

        assertThat(repository.exists(id)).isTrue();
        verify(jdbcTemplate).queryForObject(
                eq("SELECT COUNT(*) FROM chat_sessions WHERE id = ?"),
                eq(Integer.class),
                eq(id.value()));
    }

    @Test
    @DisplayName("should_report_not_exists_when_count_null_or_zero")
    void should_report_not_exists_when_count_null_or_zero() {
        ChatSessionId id = ChatSessionId.generate();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(null);

        assertThat(repository.exists(id)).isFalse();
    }

    private static ChatSession mapSession(RowMapper<ChatSession> mapper, ChatSessionId id, String clientId)
            throws Exception {
        ResultSet rs = mock(ResultSet.class);
        Instant now = Instant.parse("2026-07-26T10:00:00Z");
        when(rs.getString("id")).thenReturn(id.value());
        when(rs.getString("title")).thenReturn("Title");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));
        when(rs.getTimestamp("last_activity_at")).thenReturn(Timestamp.from(now));
        when(rs.getString("client_id")).thenReturn(clientId);
        return mapper.mapRow(rs, 0);
    }
}
