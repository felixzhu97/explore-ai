package com.ai.chat.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcChatMemorySchemaCompatibilityTest {

    @Test
    void should_fail_when_legacy_table_missing_sequence_id_column() {
        DataSource dataSource = createDataSource("legacy");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createLegacyTableWithoutSequenceId(jdbcTemplate);

        var repository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
        var chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();

        assertThatThrownBy(() -> chatMemory.add("legacy-session", new UserMessage("hello")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void should_persist_when_table_matches_spring_ai_2_schema() {
        DataSource dataSource = createDataSource("current");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createCurrentTable(jdbcTemplate);

        var repository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
        var chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();

        assertThatCode(() -> chatMemory.add("current-session", new UserMessage("hello")))
                .doesNotThrowAnyException();
        String storedType = jdbcTemplate.queryForObject(
                "SELECT type FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?",
                String.class,
                "current-session");
        assertThat(storedType).isEqualTo("USER");
    }

    private static DataSource createDataSource(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:chat-memory-" + name + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static void createLegacyTableWithoutSequenceId(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE SPRING_AI_CHAT_MEMORY (
                    conversation_id VARCHAR(36) NOT NULL,
                    content CLOB NOT NULL,
                    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);
    }

    private static void createCurrentTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
                    conversation_id VARCHAR(36) NOT NULL,
                    content LONGVARCHAR NOT NULL,
                    type VARCHAR(10) NOT NULL,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    sequence_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_SEQUENCE_ID_IDX
                ON SPRING_AI_CHAT_MEMORY(conversation_id, sequence_id)
                """);
    }
}
