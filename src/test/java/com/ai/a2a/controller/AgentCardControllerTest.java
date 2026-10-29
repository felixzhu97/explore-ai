package com.ai.a2a.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ai.a2a.domain.model.A2aSkill;
import com.ai.a2a.domain.model.AgentCard;
import com.ai.a2a.service.A2aService;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = AgentCardController.class)
@DisplayName("AgentCardController")
class AgentCardControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private A2aService a2aService;

  @Test
  @DisplayName("should return agent card json shape")
  void shouldReturnAgentCard() {
    when(a2aService.buildAgentCard(anyString()))
        .thenReturn(
            AgentCard.createCard(
                "http://localhost:9000",
                List.of(A2aSkill.createSkill("research", "Research", "facts"))));

    var json = assertThat(mvc.get().uri("/.well-known/agent-card.json")).hasStatusOk().bodyJson();
    json.extractingPath("$.name").isEqualTo("Explore AI");
    json.extractingPath("$.skills[0].id").isEqualTo("research");
    json.extractingPath("$.supportedInterfaces[0].protocolBinding").isEqualTo("HTTP+JSON");
  }
}
