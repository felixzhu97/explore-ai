package com.ai.adapter.out;

import com.ai.domain.model.ChatSession;
import com.ai.domain.vo.ChatSessionId;
import com.ai.modules.ai.infrastructure.store.InMemoryChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryChatSessionRepository Unit Tests
 * 
 * Tests the in-memory implementation of ChatSessionRepositoryPort:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests CRUD operations, clear, and size
 */
@DisplayName("InMemoryChatSessionRepository")
class InMemoryChatSessionRepositoryTest {

    private InMemoryChatSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryChatSessionRepository();
    }

    @Nested
    @DisplayName("save(ChatSession)")
    class Save {

        @Test
        @DisplayName("should store session in memory")
        void shouldStoreSessionInMemory() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");

            // Act
            repository.save(session);

            // Assert
            assertThat(repository.size()).isEqualTo(1);
            assertThat(repository.exists(session.getId())).isTrue();
        }

        @Test
        @DisplayName("should update existing session")
        void shouldUpdateExistingSession() {
            // Arrange
            ChatSession session = ChatSession.create("Original Title");
            repository.save(session);
            assertThat(repository.size()).isEqualTo(1);

            // Act - Save same session (updated)
            repository.save(session);

            // Assert
            assertThat(repository.size()).isEqualTo(1); // Still 1, not 2
        }

        @Test
        @DisplayName("should store multiple sessions")
        void shouldStoreMultipleSessions() {
            // Arrange
            ChatSession session1 = ChatSession.create("Session 1");
            ChatSession session2 = ChatSession.create("Session 2");
            ChatSession session3 = ChatSession.create("Session 3");

            // Act
            repository.save(session1);
            repository.save(session2);
            repository.save(session3);

            // Assert
            assertThat(repository.size()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("findById(ChatSessionId)")
    class FindById {

        @Test
        @DisplayName("should return session when it exists")
        void shouldReturnSessionWhenItExists() {
            // Arrange
            ChatSession session = ChatSession.create("Find Me");
            repository.save(session);

            // Act
            Optional<ChatSession> result = repository.findById(session.getId());

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Find Me");
        }

        @Test
        @DisplayName("should return empty when session does not exist")
        void shouldReturnEmptyWhenSessionDoesNotExist() {
            // Arrange
            ChatSessionId nonExistentId = ChatSessionId.generate();

            // Act
            Optional<ChatSession> result = repository.findById(nonExistentId);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when repository is empty")
        void shouldReturnEmptyWhenRepositoryIsEmpty() {
            // Act
            Optional<ChatSession> result = repository.findById(ChatSessionId.generate());

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete(ChatSessionId)")
    class Delete {

        @Test
        @DisplayName("should remove session from repository")
        void shouldRemoveSessionFromRepository() {
            // Arrange
            ChatSession session = ChatSession.create("To Be Deleted");
            repository.save(session);
            assertThat(repository.size()).isEqualTo(1);

            // Act
            repository.delete(session.getId());

            // Assert
            assertThat(repository.size()).isEqualTo(0);
            assertThat(repository.exists(session.getId())).isFalse();
        }

        @Test
        @DisplayName("should do nothing when deleting non-existent session")
        void shouldDoNothingWhenDeletingNonExistentSession() {
            // Arrange
            ChatSession session = ChatSession.create("Keep Me");
            repository.save(session);
            ChatSessionId nonExistentId = ChatSessionId.generate();

            // Act
            repository.delete(nonExistentId);

            // Assert
            assertThat(repository.size()).isEqualTo(1);
            assertThat(repository.exists(session.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all stored sessions")
        void shouldReturnAllStoredSessions() {
            // Arrange
            ChatSession session1 = ChatSession.create("Session 1");
            ChatSession session2 = ChatSession.create("Session 2");
            repository.save(session1);
            repository.save(session2);

            // Act
            List<ChatSession> results = repository.findAll();

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ChatSession::getTitle)
                    .containsExactlyInAnyOrder("Session 1", "Session 2");
        }

        @Test
        @DisplayName("should return empty list when repository is empty")
        void shouldReturnEmptyListWhenRepositoryIsEmpty() {
            // Act
            List<ChatSession> results = repository.findAll();

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return new list each time (not reference)")
        void shouldReturnNewListEachTime() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            repository.save(session);

            // Act
            List<ChatSession> list1 = repository.findAll();
            List<ChatSession> list2 = repository.findAll();

            // Assert - They should be equal but not the same reference
            assertThat(list1).isEqualTo(list2);
            assertThat(list1).isNotSameAs(list2);
        }
    }

    @Nested
    @DisplayName("exists(ChatSessionId)")
    class Exists {

        @Test
        @DisplayName("should return true when session exists")
        void shouldReturnTrueWhenSessionExists() {
            // Arrange
            ChatSession session = ChatSession.create("Existing");
            repository.save(session);

            // Act
            boolean exists = repository.exists(session.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when session does not exist")
        void shouldReturnFalseWhenSessionDoesNotExist() {
            // Act
            boolean exists = repository.exists(ChatSessionId.generate());

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("should remove all sessions")
        void shouldRemoveAllSessions() {
            // Arrange
            repository.save(ChatSession.create("Session 1"));
            repository.save(ChatSession.create("Session 2"));
            repository.save(ChatSession.create("Session 3"));
            assertThat(repository.size()).isEqualTo(3);

            // Act
            repository.clear();

            // Assert
            assertThat(repository.size()).isEqualTo(0);
            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("should be safe to call on empty repository")
        void shouldBeSafeToCallOnEmptyRepository() {
            // Act & Assert - should not throw
            repository.clear();
            assertThat(repository.size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("size()")
    class Size {

        @Test
        @DisplayName("should return zero for empty repository")
        void shouldReturnZeroForEmptyRepository() {
            // Act
            int size = repository.size();

            // Assert
            assertThat(size).isZero();
        }

        @Test
        @DisplayName("should return correct count after saves")
        void shouldReturnCorrectCountAfterSaves() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            repository.save(session);

            // Act
            int size = repository.size();

            // Assert
            assertThat(size).isEqualTo(1);
        }

        @Test
        @DisplayName("should reflect count after delete")
        void shouldReflectCountAfterDelete() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            repository.save(session);
            assertThat(repository.size()).isEqualTo(1);

            // Act
            repository.delete(session.getId());

            // Assert
            assertThat(repository.size()).isZero();
        }

        @Test
        @DisplayName("should reflect count after clear")
        void shouldReflectCountAfterClear() {
            // Arrange
            repository.save(ChatSession.create("1"));
            repository.save(ChatSession.create("2"));
            assertThat(repository.size()).isEqualTo(2);

            // Act
            repository.clear();

            // Assert
            assertThat(repository.size()).isZero();
        }
    }

    @Nested
    @DisplayName("Concurrent Access Simulation")
    class ConcurrentAccess {

        @Test
        @DisplayName("should handle multiple operations correctly")
        void shouldHandleMultipleOperationsCorrectly() {
            // Arrange - Save multiple sessions
            ChatSession session1 = ChatSession.create("First");
            ChatSession session2 = ChatSession.create("Second");
            repository.save(session1);
            repository.save(session2);

            // Act - Find, verify, delete
            Optional<ChatSession> found = repository.findById(session1.getId());
            boolean exists = repository.exists(session2.getId());
            repository.delete(session1.getId());

            // Assert
            assertThat(found).isPresent();
            assertThat(exists).isTrue();
            assertThat(repository.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should preserve remaining sessions after deletion")
        void shouldPreserveRemainingSessionsAfterDeletion() {
            // Arrange
            ChatSession session1 = ChatSession.create("Keep");
            ChatSession session2 = ChatSession.create("Remove");
            repository.save(session1);
            repository.save(session2);

            // Act
            repository.delete(session2.getId());

            // Assert
            assertThat(repository.findAll()).hasSize(1);
            assertThat(repository.findAll().get(0).getTitle()).isEqualTo("Keep");
        }
    }
}
