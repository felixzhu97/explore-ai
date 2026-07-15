package com.ai.agent.web;

import com.ai.agent.application.usecase.AgentFacade;
import com.ai.agent.domain.exception.AgentNotFoundException;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.vo.AgentType;
import com.ai.agent.web.dto.AgentInvokeRequest;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock
    private AgentFacade agentFacade;

    private AgentController controller;

    @BeforeEach
    void setUp() {
        controller = new AgentController(agentFacade);
    }

    @Test
    void should_list_agents() {
        when(agentFacade.listAgents()).thenReturn(List.of(
                AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys")));

        ResponseEntity<?> response = controller.listAgents();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).asList().hasSize(2);
    }

    @Test
    void should_return_404_when_health_unknown() {
        when(agentFacade.health("missing"))
                .thenThrow(new AgentNotFoundException(AgentType.of("missing")));

        ResponseEntity<?> response = controller.health("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return_ok_for_known_agent_health() {
        when(agentFacade.health("k8s")).thenReturn(
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "sys"));

        ResponseEntity<?> response = controller.health("k8s");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_stream_supervisor_sse() {
        when(agentFacade.invokeSupervisor("hello")).thenReturn(Flux.just(
                ServerSentEvent.<String>builder().event("message").data("hi").build(),
                ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));

        Flux<ServerSentEvent<String>> flux =
                controller.invokeSupervisor(new AgentInvokeRequest("hello", null, null));

        StepVerifier.create(flux)
                .assertNext(e -> assertThat(e.event()).isEqualTo("message"))
                .assertNext(e -> assertThat(e.event()).isEqualTo("done"))
                .verifyComplete();
    }
}
