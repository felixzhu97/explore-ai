package com.ai.pipeline.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ai.account.controller.OwnerContext;
import com.ai.common.controller.ClientIdentity;
import com.ai.pipeline.controller.dto.AgentInvokeRequest;
import com.ai.pipeline.domain.exception.AgentNotFoundException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.service.usecase.PipelineFacade;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PipelineControllerTest {
  private OwnerContext ownerContext;

  private static final String CLIENT_ID = "c:client-1";

  @Mock private PipelineFacade agentFacade;

  @Mock private HttpServletRequest httpRequest;

  private PipelineController controller;

  @BeforeEach
  void setUp() {
    ownerContext = mock(OwnerContext.class);
    lenient().when(ownerContext.requireValue(any())).thenReturn(CLIENT_ID);
    controller = new PipelineController(agentFacade, ownerContext);
    lenient()
        .when(httpRequest.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE))
        .thenReturn(CLIENT_ID);
  }

  @Test
  void shouldListAgents() {
    when(agentFacade.listAgents(eq(CLIENT_ID), anyString()))
        .thenReturn(
            List.of(
                AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys")));

    ResponseEntity<?> response = controller.listAgents(null, httpRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).asList().hasSize(2);
  }

  @Test
  void shouldReturn404WhenHealthUnknown() {
    when(agentFacade.health(eq("missing"), eq(CLIENT_ID), anyString()))
        .thenThrow(new AgentNotFoundException(AgentType.of("missing")));

    ResponseEntity<?> response = controller.health("missing", null, httpRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturnOkForKnownAgentHealth() {
    when(agentFacade.health(eq("k8s"), eq(CLIENT_ID), anyString()))
        .thenReturn(AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

    ResponseEntity<?> response = controller.health("k8s", null, httpRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturn404WhenGetAgentUnknown() {
    when(agentFacade.health(eq("missing"), eq(CLIENT_ID), anyString()))
        .thenThrow(new AgentNotFoundException(AgentType.of("missing")));

    ResponseEntity<?> response = controller.getAgent("missing", null, httpRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturnOkWhenGetAgentKnown() {
    when(agentFacade.health(eq("k8s"), eq(CLIENT_ID), anyString()))
        .thenReturn(AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

    ResponseEntity<?> response = controller.getAgent("k8s", null, httpRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldStreamSupervisorSse() {
    when(agentFacade.invokeSupervisor(eq("hello"), eq(CLIENT_ID), anyString()))
        .thenReturn(
            Flux.just(
                ServerSentEvent.<String>builder().event("message").data("hi").build(),
                ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));

    Flux<ServerSentEvent<String>> flux =
        controller.invokeSupervisor(new AgentInvokeRequest("hello", null, null), null, httpRequest);

    StepVerifier.create(flux)
        .assertNext(e -> assertThat(e.event()).isEqualTo("message"))
        .assertNext(e -> assertThat(e.event()).isEqualTo("done"))
        .verifyComplete();
  }

  @Test
  void shouldEmitErrorSseWhenDirectInvokeUnknown() {
    when(agentFacade.invokeAgent(eq("missing"), eq("hi"), eq(CLIENT_ID), anyString()))
        .thenReturn(Flux.error(new AgentNotFoundException(AgentType.of("missing"))));

    Flux<ServerSentEvent<String>> flux =
        controller.invokeAgent(
            "missing", new AgentInvokeRequest("hi", null, null), null, httpRequest);

    StepVerifier.create(flux)
        .assertNext(e -> assertThat(e.event()).isEqualTo("error"))
        .assertNext(e -> assertThat(e.event()).isEqualTo("done"))
        .verifyComplete();
  }

  @Test
  void shouldReportModuleHealth() {
    when(agentFacade.builtinCount()).thenReturn(1);

    ResponseEntity<?> response = controller.moduleHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isInstanceOf(java.util.Map.class);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
    assertThat(body).containsEntry("status", "UP").containsEntry("agents", 1);
  }
}
