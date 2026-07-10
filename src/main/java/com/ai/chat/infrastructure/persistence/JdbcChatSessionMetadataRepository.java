package com.ai.chat.infrastructure.persistence;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence for chat session metadata (messages stored in ChatMemory).
 */
@Repository
public class JdbcChatSessionMetadataRepository implements ChatSessionRepository {

    private static final RowMapper<ChatSession> ROW_MAPPER = (rs, rowNum) -> {
        ChatSessionId id = ChatSessionId.of(rs.getString("id"));
        String title = rs.getString("title");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant lastActivityAt = rs.getTimestamp("last_activity_at").toInstant();
        return ChatSession.reconstitute(id, title, createdAt, lastActivityAt, List.of());
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatSessionMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ChatSession> findById(ChatSessionId id) {
        List<ChatSession> results = jdbcTemplate.query(
                "SELECT id, title, created_at, last_activity_at FROM chat_sessions WHERE id = ?",
                ROW_MAPPER,
                id.value());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    @Transactional
    public void save(ChatSession session) {
        jdbcTemplate.update(
                """
                MERGE INTO chat_sessions (id, title, created_at, last_activity_at)
                KEY (id)
                VALUES (?, ?, ?, ?)
                """,
                session.getId().value(),
                session.getTitle(),
                Timestamp.from(session.getCreatedAt()),
                Timestamp.from(session.getLastActivityAt()));
    }

    @Override
    @Transactional
    public void delete(ChatSessionId id) {
        jdbcTemplate.update("DELETE FROM chat_sessions WHERE id = ?", id.value().toString());
    }

    @Override
    public List<ChatSession> findAll() {
        return jdbcTemplate.query(
                "SELECT id, title, created_at, last_activity_at FROM chat_sessions ORDER BY last_activity_at DESC",
                ROW_MAPPER);
    }

    @Override
    public boolean exists(ChatSessionId id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_sessions WHERE id = ?",
                Integer.class,
                id.value());
        return count != null && count > 0;
    }
}
