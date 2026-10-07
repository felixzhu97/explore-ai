package com.ai.pipeline.controller;

import static com.ai.testsupport.MvcStreamTestSupport.exchangeStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.service.PipelineService;
import com.ai.testsupport.AbstractOwnerScopedControllerTest;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

@SliceWebMvcTest(controllers = PipelineController.class)
@DisplayName("PipelineController")
class PipelineControllerTest extends AbstractOwnerScopedControllerTest {

  @MockitoBean private PipelineService pipelineService;

  @Nested
  @DisplayName("GET /api/pipelines/agent-types")
  class ListAgents {

    @Test
    @DisplayName("should list agents")
    void shouldListAgents() {
      when(pipelineService.listAgents(eq(ownerKey()), anyString()))
          .thenReturn(
              List.of(
                  AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                  AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys")));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/agent-types")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$")
          .asArray()
          .hasSize(2);
    }
  }

  @Nested
  @DisplayName("GET /api/pipelines/{agentType}/health")
  class AgentHealth {

    @Test
    @DisplayName("should return 404 when health unknown")
    void shouldReturn404WhenHealthUnknown() {
      when(pipelineService.getHealth(eq("missing"), eq(ownerKey()), anyString()))
          .thenThrow(
              DomainException.notFound(
                  "AGENT_NOT_FOUND", "Unknown agent type: " + AgentType.of("missing").value()));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/missing/health")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should return ok for known agent health")
    void shouldReturnOkForKnownAgentHealth() {
      when(pipelineService.getHealth(eq("k8s"), eq(ownerKey()), anyString()))
          .thenReturn(AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/k8s/health")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatusOk();
    }
  }

  @Nested
  @DisplayName("GET /api/pipelines/{agentType}")
  class GetAgent {

    @Test
    @DisplayName("should return 404 when agent unknown")
    void shouldReturn404WhenGetAgentUnknown() {
      when(pipelineService.getHealth(eq("missing"), eq(ownerKey()), anyString()))
          .thenThrow(
              DomainException.notFound(
                  "AGENT_NOT_FOUND", "Unknown agent type: " + AgentType.of("missing").value()));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/missing")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should return ok when agent known")
    void shouldReturnOkWhenGetAgentKnown() {
      when(pipelineService.getHealth(eq("k8s"), eq(ownerKey()), anyString()))
          .thenReturn(AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/k8s")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatusOk();
    }
  }

  @Nested
  @DisplayName("POST /api/pipelines/supervisor/invoke/sse")
  class InvokeSupervisor {

    @Test
    @DisplayName("should stream supervisor SSE")
    void shouldStreamSupervisorSse() {
      when(pipelineService.invokeSupervisor(eq("hello"), eq(ownerKey()), anyString()))
          .thenReturn(
              Flux.just(
                  ServerSentEvent.<String>builder().event("message").data("hi").build(),
                  ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));

      assertThat(
              exchangeStream(
                  mvc.post()
                      .uri("/api/pipelines/supervisor/invoke/sse")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"message\":\"hello\"}")
                      .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey()))))
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("hi")
          .contains("[DONE]");
    }
  }

  @Nested
  @DisplayName("POST /api/pipelines/{agentType}/invoke/sse")
  class InvokeAgent {

    @Test
    @DisplayName("should reject blank message")
    void shouldRejectBlankMessage() {
      assertThat(
              mvc.post()
                  .uri("/api/pipelines/k8s/invoke/sse")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":\" \"}")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatus(400);
    }
  }

  @Nested
  @DisplayName("GET /api/pipelines/health")
  class ModuleHealth {

    @Test
    @DisplayName("should report module health")
    void shouldReportModuleHealth() {
      when(pipelineService.countBuiltins()).thenReturn(1);

      assertThat(mvc.get().uri("/api/pipelines/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.status")
          .asString()
          .isEqualTo("UP");

      assertThat(mvc.get().uri("/api/pipelines/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.agents")
          .convertTo(Integer.class)
          .isEqualTo(1);
    }
  }
}
