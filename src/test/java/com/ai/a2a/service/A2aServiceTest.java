package com.ai.a2a.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.a2a.domain.model.A2aMessage;
import com.ai.a2a.domain.model.A2aTask;
import com.ai.a2a.domain.model.A2aTaskStatus;
import com.ai.a2a.domain.model.AgentCard;
import com.ai.agent.domain.model.AgentRun;
import com.ai.agent.service.AgentService;
import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.pipeline.domain.repository.AgentRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("A2aService")
class A2aServiceTest {

  @Mock private AgentService agentService;
  @Mock private AgentRegistry agentRegistry;
  @InjectMocks private A2aService a2aService;

  @Test
  @DisplayName("should build agent card from builtin workers only")
  void shouldBuildAgentCard() {
    when(agentRegistry.listBuiltins("en"))
        .thenReturn(
            List.of(
                AgentDefinition.createDefinition(
                    AgentType.createType("research"), "Research", "facts", "sys"),
                AgentDefinition.createDefinition(
                    AgentType.createSupervisorType(), "Supervisor", "coords", "sys")));

    AgentCard card = a2aService.buildAgentCard("http://localhost:9000");

    assertThat(card.getSkills()).hasSize(1);
    assertThat(card.getSkills().getFirst().getId()).isEqualTo("research");
  }

  @Test
  @DisplayName("should map send message to completed task via AgentService")
  void shouldSendMessage() {
    when(agentService.invoke(eq("research"), eq("hello"), eq("owner"), eq("en")))
        .thenReturn(AgentRun.createCompleted("research", "hello", "world"));

    A2aTask task =
        a2aService.sendMessage(
            A2aMessage.createMessage("m1", "user", List.of(Map.of("text", "hello")), Map.of()),
            "owner",
            "en");

    assertThat(task.getStatus()).isEqualTo(A2aTaskStatus.COMPLETED);
    assertThat(task.getArtifacts()).isNotEmpty();
  }

  @Test
  @DisplayName("should sanitize failure messages without echoing provider text")
  void shouldSanitizeFailureMessages() {
    assertThat(
            A2aService.safeFailureMessage(
                DomainException.createNotFoundError("AGENT_NOT_FOUND", "Unknown agent")))
        .isEqualTo("AGENT_NOT_FOUND");
    assertThat(A2aService.safeFailureMessage(new RuntimeException("prompt leaked")))
        .isEqualTo("agent_invoke_failed");
  }
}
