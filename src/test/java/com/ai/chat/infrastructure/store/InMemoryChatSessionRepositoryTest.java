package com.ai.chat.infrastructure.store;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryChatSessionRepository Tests")
class InMemoryChatSessionRepositoryTest {

    private InMemoryChatSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryChatSessionRepository();
    }

    @Nested
    @DisplayName("Save and FindById Tests")
    class SaveAndFindByIdTests {

        @Test
        @DisplayName("should find saved session by id")
        void shouldFindSavedSessionById() {
            // Given
            var session = ChatSession.create("Test Session", "client-a");
            repository.save(session);

            // When
            Optional<ChatSession> result = repository.findById(session.getId());

            // Then
            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(s -> assertThat(s.getId()).isEqualTo(session.getId()));
        }

        @Test
        @DisplayName("should return empty when session not found")
        void shouldReturnEmptyWhenSessionNotFound() {
            // Given
            var nonExistentId = ChatSessionId.generate();

            // When
            Optional<ChatSession> result = repository.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should update existing session when saving with same id")
        void shouldUpdateExistingSessionWhenSavingWithSameId() {
            // Given
            var session1 = ChatSession.create("Test Session", "client-a");
            repository.save(session1);

            // When - Save another session (would need to simulate update scenario)
            repository.save(session1);

            // Then
            assertThat(repository.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("should remove session when deleted")
        void shouldRemoveSessionWhenDeleted() {
            // Given
            var session = ChatSession.create("Test Session", "client-a");
            repository.save(session);
            assertThat(repository.exists(session.getId())).isTrue();

            // When
            repository.delete(session.getId());

            // Then
            assertThat(repository.exists(session.getId())).isFalse();
            assertThat(repository.findById(session.getId())).isEmpty();
        }

        @Test
        @DisplayName("should not throw when deleting non-existent session")
        void shouldNotThrowWhenDeletingNonExistentSession() {
            // Given
            var nonExistentId = ChatSessionId.generate();

            // When & Then - should not throw
            repository.delete(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Exists Tests")
    class ExistsTests {

        @Test
        @DisplayName("should return true for existing session")
        void shouldReturnTrueForExistingSession() {
            // Given
            var session = ChatSession.create("Test Session", "client-a");
            repository.save(session);

            // When & Then
            assertThat(repository.exists(session.getId())).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existing session")
        void shouldReturnFalseForNonExistingSession() {
            // Given
            var nonExistentId = ChatSessionId.generate();

            // When & Then
            assertThat(repository.exists(nonExistentId)).isFalse();
        }
    }

    @Nested
    @DisplayName("FindByClientId Tests")
    class FindByClientIdTests {

        @Test
        @DisplayName("should return empty list when no sessions exist")
        void shouldReturnEmptyListWhenNoSessionsExist() {
            List<ChatSession> result = repository.findByClientId("client-a");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return only sessions for the client")
        void shouldReturnOnlyOwnedSessionsWhenMultipleClientsExist() {
            var session1 = ChatSession.create("Test Session 1", "client-a");
            var session2 = ChatSession.create("Test Session 2", "client-a");
            var other = ChatSession.create("Other", "client-b");
            repository.save(session1);
            repository.save(session2);
            repository.save(other);

            List<ChatSession> result = repository.findByClientId("client-a");

            assertThat(result)
                    .hasSize(2)
                    .extracting(ChatSession::getId)
                    .containsExactlyInAnyOrder(session1.getId(), session2.getId());
        }

        @Test
        @DisplayName("should return empty when looking up by other client id")
        void shouldReturnEmptyWhenSessionBelongsToOtherClient() {
            var session = ChatSession.create("Owned", "client-a");
            repository.save(session);

            assertThat(repository.findByIdAndClientId(session.getId(), "client-b")).isEmpty();
            assertThat(repository.findByIdAndClientId(session.getId(), "client-a")).isPresent();
        }
    }

    @Nested
    @DisplayName("Clear Tests")
    class ClearTests {

        @Test
        @DisplayName("should clear all sessions")
        void shouldClearAllSessions() {
            // Given
            var session1 = ChatSession.create("Test Session 1", "client-a");
            var session2 = ChatSession.create("Test Session 2", "client-a");
            repository.save(session1);
            repository.save(session2);
            assertThat(repository.size()).isEqualTo(2);

            // When
            repository.clear();

            // Then
            assertThat(repository.size()).isZero();
            assertThat(repository.findByClientId("client-a")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Size Tests")
    class SizeTests {

        @Test
        @DisplayName("should return zero for empty repository")
        void shouldReturnZeroForEmptyRepository() {
            // When & Then
            assertThat(repository.size()).isZero();
        }

        @Test
        @DisplayName("should return correct count after saving sessions")
        void shouldReturnCorrectCountAfterSavingSessions() {
            // Given
            var session1 = ChatSession.create("Test Session 1", "client-a");
            var session2 = ChatSession.create("Test Session 2", "client-a");

            // When
            repository.save(session1);
            assertThat(repository.size()).isEqualTo(1);

            repository.save(session2);
            assertThat(repository.size()).isEqualTo(2);
        }
    }
}
