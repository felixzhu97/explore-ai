package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentSession Tests")
class AgentSessionTest {

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create session with agent id")
        void shouldCreateSessionWithAgentId() {
            AgentId agentId = AgentId.of("chat-1");

            AgentSession session = AgentSession.create(agentId);

            assertThat(session.agentId()).isEqualTo(agentId);
            assertThat(session.status()).isEqualTo(AgentSession.SessionStatus.ACTIVE);
            assertThat(session.sessionData()).isEmpty();
        }

        @Test
        @DisplayName("should create session with agent id and initial data")
        void shouldCreateSessionWithAgentIdAndInitialData() {
            AgentId agentId = AgentId.of("chat-1");
            Map<String, Object> initialData = Map.of("key", "value");

            AgentSession session = AgentSession.create(agentId, initialData);

            assertThat(session.agentId()).isEqualTo(agentId);
            assertThat(session.sessionData()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("should generate unique session id")
        void shouldGenerateUniqueSessionId() {
            AgentId agentId = AgentId.of("chat-1");

            AgentSession session1 = AgentSession.create(agentId);
            AgentSession session2 = AgentSession.create(agentId);

            assertThat(session1.id()).isNotEqualTo(session2.id());
        }

        @Test
        @DisplayName("should set createdAt and lastAccessedAt to now")
        void shouldSetCreatedAtAndLastAccessedAtToNow() {
            Instant before = Instant.now().minusSeconds(1);
            AgentId agentId = AgentId.of("chat-1");

            AgentSession session = AgentSession.create(agentId);

            Instant after = Instant.now().plusSeconds(1);
            assertThat(session.createdAt()).isAfterOrEqualTo(before);
            assertThat(session.createdAt()).isBeforeOrEqualTo(after);
            assertThat(session.lastAccessedAt()).isAfterOrEqualTo(before);
            assertThat(session.lastAccessedAt()).isBeforeOrEqualTo(after);
        }
    }

    @Nested
    @DisplayName("updateActivity method")
    class UpdateActivityMethodTests {

        @Test
        @DisplayName("should update lastAccessedAt timestamp")
        void shouldUpdateLastAccessedAtTimestamp() throws InterruptedException {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);
            Instant originalLastAccessed = session.lastAccessedAt();

            Thread.sleep(10);
            AgentSession updatedSession = session.updateActivity();

            assertThat(updatedSession.lastAccessedAt()).isAfter(originalLastAccessed);
        }

        @Test
        @DisplayName("should preserve other session properties")
        void shouldPreserveOtherSessionProperties() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId, Map.of("key", "value"));

            AgentSession updatedSession = session.updateActivity();

            assertThat(updatedSession.agentId()).isEqualTo(session.agentId());
            assertThat(updatedSession.sessionData()).isEqualTo(session.sessionData());
            assertThat(updatedSession.status()).isEqualTo(session.status());
        }
    }

    @Nested
    @DisplayName("putData method")
    class PutDataMethodTests {

        @Test
        @DisplayName("should add data to session")
        void shouldAddDataToSession() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);

            AgentSession updatedSession = session.putData("newKey", "newValue");

            assertThat(updatedSession.getData("newKey")).isEqualTo("newValue");
        }

        @Test
        @DisplayName("should update existing data")
        void shouldUpdateExistingData() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId, Map.of("key", "oldValue"));

            AgentSession updatedSession = session.putData("key", "newValue");

            assertThat(updatedSession.getData("key")).isEqualTo("newValue");
        }

        @Test
        @DisplayName("should update lastAccessedAt when putting data")
        void shouldUpdateLastAccessedAtWhenPuttingData() throws InterruptedException {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);
            Instant originalLastAccessed = session.lastAccessedAt();

            Thread.sleep(10);
            AgentSession updatedSession = session.putData("key", "value");

            assertThat(updatedSession.lastAccessedAt()).isAfter(originalLastAccessed);
        }
    }

    @Nested
    @DisplayName("putAllData method")
    class PutAllDataMethodTests {

        @Test
        @DisplayName("should add multiple data entries")
        void shouldAddMultipleDataEntries() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);
            Map<String, Object> newData = Map.of("key1", "value1", "key2", "value2");

            AgentSession updatedSession = session.putAllData(newData);

            assertThat(updatedSession.getData("key1")).isEqualTo("value1");
            assertThat(updatedSession.getData("key2")).isEqualTo("value2");
        }

        @Test
        @DisplayName("should preserve existing data when adding more")
        void shouldPreserveExistingDataWhenAddingMore() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId, Map.of("existing", "data"));
            Map<String, Object> newData = Map.of("new", "data");

            AgentSession updatedSession = session.putAllData(newData);

            assertThat(updatedSession.getData("existing")).isEqualTo("data");
            assertThat(updatedSession.getData("new")).isEqualTo("data");
        }
    }

    @Nested
    @DisplayName("close method")
    class CloseMethodTests {

        @Test
        @DisplayName("should set status to CLOSED")
        void shouldSetStatusToClosed() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);

            AgentSession closedSession = session.close();

            assertThat(closedSession.status()).isEqualTo(AgentSession.SessionStatus.CLOSED);
        }

        @Test
        @DisplayName("should preserve other properties when closing")
        void shouldPreserveOtherPropertiesWhenClosing() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId, Map.of("key", "value"));

            AgentSession closedSession = session.close();

            assertThat(closedSession.agentId()).isEqualTo(agentId);
            assertThat(closedSession.getData("key")).isEqualTo("value");
        }
    }

    @Nested
    @DisplayName("pause method")
    class PauseMethodTests {

        @Test
        @DisplayName("should set status to PAUSED")
        void shouldSetStatusToPaused() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);

            AgentSession pausedSession = session.pause();

            assertThat(pausedSession.status()).isEqualTo(AgentSession.SessionStatus.PAUSED);
        }
    }

    @Nested
    @DisplayName("isActive method")
    class IsActiveMethodTests {

        @Test
        @DisplayName("should return true for active session")
        void shouldReturnTrueForActiveSession() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);

            assertThat(session.isActive()).isTrue();
        }

        @Test
        @DisplayName("should return false for closed session")
        void shouldReturnFalseForClosedSession() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId).close();

            assertThat(session.isActive()).isFalse();
        }

        @Test
        @DisplayName("should return false for paused session")
        void shouldReturnFalseForPausedSession() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId).pause();

            assertThat(session.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired method")
    class IsExpiredMethodTests {

        @Test
        @DisplayName("should return false for recently accessed session")
        void shouldReturnFalseForRecentlyAccessedSession() {
            AgentId agentId = AgentId.of("chat-1");
            AgentSession session = AgentSession.create(agentId);

            assertThat(session.isExpired(30)).isFalse();
        }
    }

    @Nested
    @DisplayName("SessionId inner class")
    class SessionIdTests {

        @Test
        @DisplayName("should generate unique session ids")
        void shouldGenerateUniqueSessionIds() {
            var id1 = AgentSession.create(AgentId.of("c1")).id();
            var id2 = AgentSession.create(AgentId.of("c1")).id();

            assertThat(id1).isNotEqualTo(id2);
        }
    }
}
