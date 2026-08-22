package com.ai.pipeline.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ai.account.controller.OwnerContext;
import com.ai.pipeline.domain.exception.AgentNotFoundException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.service.usecase.PipelineFacade;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import reactor.core.publisher.Flux;

@SliceWebMvcTest(controllers = PipelineController.class)
@DisplayName("PipelineController")
class PipelineControllerTest {

  private static final String CLIENT_ID = "c:client-1";

  @Autowired private MockMvcTester mvc;

  @MockitoBean private PipelineFacade agentFacade;

  @MockitoBean private OwnerContext ownerContext;

  @BeforeEach
  void setUp() {
    lenient().when(ownerContext.requireValue(any())).thenReturn(CLIENT_ID);
  }

  @Nested
  @DisplayName("GET /api/pipelines/list")
  class ListAgents {

    @Test
    @DisplayName("should list agents")
    void shouldListAgents() {
      when(agentFacade.listAgents(eq(CLIENT_ID), anyString()))
          .thenReturn(
              List.of(
                  AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                  AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys")));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/list")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
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
      when(agentFacade.health(eq("missing"), eq(CLIENT_ID), anyString()))
          .thenThrow(new AgentNotFoundException(AgentType.of("missing")));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/missing/health")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should return ok for known agent health")
    void shouldReturnOkForKnownAgentHealth() {
      when(agentFacade.health(eq("k8s"), eq(CLIENT_ID), anyString()))
          .thenReturn(AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/k8s/health")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatusOk();
    }
  }

  @Nested
  @DisplayName("GET /api/pipelines/{agentType}")
  class GetAgent {

    @Test
    @DisplayName("should return 404 when agent unknown")
    void shouldReturn404WhenGetAgentUnknown() {
      when(agentFacade.health(eq("missing"), eq(CLIENT_ID), anyString()))
          .thenThrow(new AgentNotFoundException(AgentType.of("missing")));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/missing")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should return ok when agent known")
    void shouldReturnOkWhenGetAgentKnown() {
      when(agentFacade.health(eq("k8s"), eq(CLIENT_ID), anyString()))
          .thenReturn(AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

      assertThat(
              mvc.get()
                  .uri("/api/pipelines/k8s")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatusOk();
    }
  }

  @Nested
  @DisplayName("POST /api/pipelines/supervisor/invoke/sse")
  class InvokeSupervisor {

    @Test
    @DisplayName("should stream supervisor SSE")
    void shouldStreamSupervisorSse() {
      when(agentFacade.invokeSupervisor(eq("hello"), eq(CLIENT_ID), anyString()))
          .thenReturn(
              Flux.just(
                  ServerSentEvent.<String>builder().event("message").data("hi").build(),
                  ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));

      assertThat(
              mvc.post()
                  .uri("/api/pipelines/supervisor/invoke/sse")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":\"hello\"}")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID))
                  .asyncExchange())
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("event:message")
          .contains("event:done");
    }
  }

  @Nested
  @DisplayName("POST /api/pipelines/{agentType}/invoke/sse")
  class InvokeAgent {

    @Test
    @DisplayName("should emit error SSE when direct invoke unknown")
    void shouldEmitErrorSseWhenDirectInvokeUnknown() {
      when(agentFacade.invokeAgent(eq("missing"), eq("hi"), eq(CLIENT_ID), anyString()))
          .thenReturn(Flux.error(new AgentNotFoundException(AgentType.of("missing"))));

      assertThat(
              mvc.post()
                  .uri("/api/pipelines/missing/invoke/sse")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":\"hi\"}")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID))
                  .asyncExchange())
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("event:error")
          .contains("event:done");
    }
  }

  @Nested
  @DisplayName("GET /api/pipelines/health")
  class ModuleHealth {

    @Test
    @DisplayName("should report module health")
    void shouldReportModuleHealth() {
      when(agentFacade.builtinCount()).thenReturn(1);

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
