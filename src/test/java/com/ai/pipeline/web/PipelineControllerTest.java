package com.ai.pipeline.web;

import com.ai.account.web.OwnerContext;
import com.ai.common.domain.vo.OwnerKey;

import com.ai.common.web.ClientIdentity;
import com.ai.pipeline.application.usecase.PipelineFacade;
import com.ai.pipeline.domain.exception.AgentNotFoundException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.web.dto.AgentInvokeRequest;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineControllerTest {
    private OwnerContext ownerContext;

    private static final String CLIENT_ID = "client-1";

    @Mock
    private PipelineFacade agentFacade;

    @Mock
    private HttpServletRequest httpRequest;

    private PipelineController controller;

    @BeforeEach
    void setUp() {
        ownerContext = mock(OwnerContext.class);
        lenient().when(ownerContext.requireValue(any())).thenReturn(CLIENT_ID);
        controller = new PipelineController(agentFacade, ownerContext);
        lenient().when(httpRequest.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE)).thenReturn(CLIENT_ID);
    }

    @Test
    void should_list_agents() {
        when(agentFacade.listAgents(eq(CLIENT_ID), anyString())).thenReturn(List.of(
                AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys")));

        ResponseEntity<?> response = controller.listAgents(null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).asList().hasSize(2);
    }

    @Test
    void should_return_404_when_health_unknown() {
        when(agentFacade.health(eq("missing"), eq(CLIENT_ID), anyString()))
                .thenThrow(new AgentNotFoundException(AgentType.of("missing")));

        ResponseEntity<?> response = controller.health("missing", null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return_ok_for_known_agent_health() {
        when(agentFacade.health(eq("k8s"), eq(CLIENT_ID), anyString())).thenReturn(
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

        ResponseEntity<?> response = controller.health("k8s", null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_return_404_when_get_agent_unknown() {
        when(agentFacade.health(eq("missing"), eq(CLIENT_ID), anyString()))
                .thenThrow(new AgentNotFoundException(AgentType.of("missing")));

        ResponseEntity<?> response = controller.getAgent("missing", null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return_ok_when_get_agent_known() {
        when(agentFacade.health(eq("k8s"), eq(CLIENT_ID), anyString())).thenReturn(
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

        ResponseEntity<?> response = controller.getAgent("k8s", null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_stream_supervisor_sse() {
        when(agentFacade.invokeSupervisor(eq("hello"), eq(CLIENT_ID), anyString())).thenReturn(Flux.just(
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
    void should_emit_error_sse_when_direct_invoke_unknown() {
        when(agentFacade.invokeAgent(eq("missing"), eq("hi"), eq(CLIENT_ID), anyString()))
                .thenReturn(Flux.error(new AgentNotFoundException(AgentType.of("missing"))));

        Flux<ServerSentEvent<String>> flux =
                controller.invokeAgent("missing", new AgentInvokeRequest("hi", null, null), null, httpRequest);

        StepVerifier.create(flux)
                .assertNext(e -> assertThat(e.event()).isEqualTo("error"))
                .assertNext(e -> assertThat(e.event()).isEqualTo("done"))
                .verifyComplete();
    }

    @Test
    void should_report_module_health() {
        when(agentFacade.builtinCount()).thenReturn(1);

        ResponseEntity<?> response = controller.moduleHealth();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("status", "UP").containsEntry("agents", 1);
    }
}
