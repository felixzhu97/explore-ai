package com.ai.a2a.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.agent.service.AgentService;
import com.ai.common.exception.DomainException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A2aMessage")
class A2aMessageTest {

  @Test
  @DisplayName("should extract text parts and default agent type to research")
  void shouldExtractTextAndDefaultAgentType() {
    A2aMessage message =
        A2aMessage.createMessage("msg-1", "user", List.of(Map.of("text", "hello")), Map.of());

    assertThat(message.extractText()).isEqualTo("hello");
    assertThat(message.resolveAgentType(AgentService.DEFAULT_AGENT_TYPE)).isEqualTo("research");
  }

  @Test
  @DisplayName("should resolve agent type from metadata skill")
  void shouldResolveAgentTypeFromSkill() {
    A2aMessage message =
        A2aMessage.createMessage(
            "msg-1", "user", List.of(Map.of("text", "weather?")), Map.of("skill", "weather"));

    assertThat(message.resolveAgentType("research")).isEqualTo("weather");
  }

  @Test
  @DisplayName("should reject message without text parts")
  void shouldRejectMissingText() {
    A2aMessage message =
        A2aMessage.createMessage("msg-1", "user", List.of(Map.of("data", "x")), Map.of());

    assertThatThrownBy(message::extractText)
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "A2A_TEXT_REQUIRED");
  }
}
