package com.ai.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.agent.domain.model.AgentRun;
import com.ai.agent.service.AgentService;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.testsupport.AbstractOwnerScopedControllerTest;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SliceWebMvcTest(controllers = AgentController.class)
@DisplayName("AgentController")
class AgentControllerTest extends AbstractOwnerScopedControllerTest {

  @MockitoBean private AgentService agentService;

  @Test
  @DisplayName("should list agents without system prompt")
  void shouldListAgents() {
    when(agentService.listAgents(eq(ownerKey()), anyString()))
        .thenReturn(
            List.of(
                AgentDefinition.createDefinition(
                    AgentType.createType("research"),
                    "Research",
                    "finds facts",
                    "secret prompt",
                    List.of("docs"),
                    AgentDefinition.RUNTIME_SINGLE)));

    var json =
        assertThat(
                mvc.get()
                    .uri("/api/agents")
                    .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
            .hasStatusOk()
            .bodyJson();
    json.extractingPath("$[0].type").isEqualTo("research");
    json.extractingPath("$[0].tools[0]").isEqualTo("docs");
  }

  @Test
  @DisplayName("should invoke agent and return reply")
  void shouldInvokeAgent() {
    when(agentService.invoke(eq("research"), eq("hi"), eq(ownerKey()), anyString()))
        .thenReturn(AgentRun.createCompleted("research", "hi", "hello"));

    assertThat(
            mvc.post()
                .uri("/api/agents/research/invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hi\"}")
                .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.reply")
        .isEqualTo("hello");
  }
}
