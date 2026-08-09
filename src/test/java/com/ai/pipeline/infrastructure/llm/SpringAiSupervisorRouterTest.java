package com.ai.pipeline.infrastructure.llm;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.RoutingPlan;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiSupervisorRouter")
class SpringAiSupervisorRouterTest {

    @Mock
    private ChatClientProvider chatClientProvider;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SpringAiSupervisorRouter router;

    @BeforeEach
    void setUp() {
        router = new SpringAiSupervisorRouter(chatClientProvider);
    }

    @Test
    @DisplayName("should throw when no workers registered")
    void shouldThrowWhenNoWorkersRegistered() {
        assertThatThrownBy(() -> router.plan("hello", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no worker agents");
    }

    @Test
    @DisplayName("should route to primary worker from json response")
    void shouldRouteToPrimaryWorkerFromJsonResponse() {
        AgentDefinition researcher = AgentDefinition.create(
                AgentType.of("researcher"),
                "Researcher",
                "Finds facts",
                "You research.");
        when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("""
                {
                  "primaryAgent": "researcher",
                  "reason": "needs lookup",
                  "subtasks": []
                }
                """);

        RoutingPlan plan = router.plan("find market data", List.of(researcher));

        assertThat(plan.primaryAgent()).isEqualTo(AgentType.of("researcher"));
        assertThat(plan.reason()).isEqualTo("needs lookup");
    }

    @Test
    @DisplayName("should fallback when primary agent invalid")
    void shouldFallbackWhenPrimaryAgentInvalid() {
        AgentDefinition writer = AgentDefinition.create(
                AgentType.of("writer"),
                "Writer",
                "Writes prose",
                "You write.");
        when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("""
                {
                  "primaryAgent": "supervisor",
                  "reason": "",
                  "subtasks": [
                    {"agentType": "writer", "instruction": "Draft summary"}
                  ]
                }
                """);

        RoutingPlan plan = router.plan("summarize", List.of(writer));

        assertThat(plan.primaryAgent()).isEqualTo(AgentType.of("writer"));
        assertThat(plan.subtasks()).hasSize(1);
    }

    @Test
    @DisplayName("should fallback when decision null")
    void shouldFallbackWhenDecisionNull() {
        AgentDefinition writer = AgentDefinition.create(
                AgentType.of("writer"), "Writer", "Writes prose", "You write.");
        when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{}");

        RoutingPlan plan = router.plan("summarize", List.of(writer));

        assertThat(plan.primaryAgent()).isEqualTo(AgentType.of("writer"));
        assertThat(plan.reason()).contains("fallback");
    }
}
