package com.ai.domain.model;

import com.ai.domain.vo.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatMessage")
class ChatMessageTest {

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when same id")
        void shouldBeEqualWhenSameId() {
            var id = MessageId.generate();
            Instant timestamp = Instant.now();
            ChatMessage msg1 = ChatMessage.of(id, "Hello", "user", timestamp);
            ChatMessage msg2 = ChatMessage.of(id, "Hello", "user", timestamp);

            assertThat(msg1).isEqualTo(msg2);
            assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when different id")
        void shouldNotBeEqualWhenDifferentId() {
            ChatMessage msg1 = ChatMessage.createUserMessage("Hello");
            ChatMessage msg2 = ChatMessage.createUserMessage("Hello");

            assertThat(msg1).isNotEqualTo(msg2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            ChatMessage msg = ChatMessage.createUserMessage("Hello");
            assertThat(msg).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            ChatMessage msg = ChatMessage.createUserMessage("Hello");
            assertThat(msg).isNotEqualTo("not a message");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain id and role in toString")
        void shouldContainIdAndRoleInToString() {
            var id = MessageId.of("fixed-id");
            Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");
            ChatMessage msg = ChatMessage.of(id, "Hello", "user", timestamp);

            String str = msg.toString();
            assertThat(str).contains("fixed-id");
            assertThat(str).contains("user");
            assertThat(str).contains("2024-01-01T00:00:00Z");
        }
    }
}
