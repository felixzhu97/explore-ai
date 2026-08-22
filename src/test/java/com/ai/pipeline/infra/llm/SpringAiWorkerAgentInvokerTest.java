package com.ai.pipeline.infra.llm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.repository.DateTimeTool;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infra.skills.AgentSkillsRuntime;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SpringAiWorkerAgentInvokerTest {

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private DocumentSearchTool documentSearchTool;
  @Mock private WebSearchTool webSearchTool;
  @Mock private WeatherTool weatherTool;
  @Mock private DateTimeTool dateTimeTool;
  @Mock private AgentSkillsRuntime agentSkillsRuntime;
  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  @Test
  void shouldBindWeatherToolWhenWeatherAgentInvoked() {
    SpringAiWorkerAgentInvoker invoker = newInvoker();
    stubCallChain();

    invoker.invoke(agent("weather"), "Beijing weather");

    verify(requestSpec).tools(eq(weatherTool));
  }

  @Test
  void shouldBindWebSearchAndDatetimeToolsWhenResearchAgentInvoked() {
    SpringAiWorkerAgentInvoker invoker = newInvoker();
    stubCallChain();

    invoker.invoke(agent("research"), "latest Spring AI release");

    verify(requestSpec).tools(eq(webSearchTool), eq(dateTimeTool));
  }

  @Test
  void shouldBindDocumentSearchToolWhenVectordbAgentInvoked() {
    SpringAiWorkerAgentInvoker invoker = newInvoker();
    stubCallChain();

    invoker.invoke(agent("vectordb"), "find onboarding docs");

    verify(requestSpec).tools(eq(documentSearchTool));
  }

  @Test
  void shouldSkipToolsWhenAnalystAgentInvoked() {
    SpringAiWorkerAgentInvoker invoker = newInvoker();
    stubCallChainWithoutTools();

    invoker.invoke(agent("analyst"), "summarize findings");

    verify(requestSpec, never()).tools(any());
    verify(requestSpec).call();
  }

  @Test
  void shouldUseBlockingCallWhenStreamingToolAgent() {
    SpringAiWorkerAgentInvoker invoker = newInvoker();
    stubCallChain();
    when(callResponseSpec.content())
        .thenReturn("brief <｜DSML｜tool_calls>leak</｜DSML｜tool_calls> done");

    StepVerifier.create(invoker.invokeStream(agent("research"), "search topic"))
        .expectNext("brief  done")
        .verifyComplete();

    verify(requestSpec).call();
    verify(requestSpec, never()).stream();
  }

  private SpringAiWorkerAgentInvoker newInvoker() {
    lenient()
        .when(agentSkillsRuntime.augmentSystemPrompt(any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    return new SpringAiWorkerAgentInvoker(
        chatClientProvider,
        documentSearchTool,
        webSearchTool,
        weatherTool,
        dateTimeTool,
        agentSkillsRuntime);
  }

  private static AgentDefinition agent(String type) {
    java.util.List<String> tools =
        switch (type) {
          case "weather" -> java.util.List.of("weather");
          case "research" -> java.util.List.of("web", "datetime");
          case "vectordb" -> java.util.List.of("document");
          default -> java.util.List.of();
        };
    return AgentDefinition.create(AgentType.of(type), type, type, "system", tools, "single");
  }

  private void stubCallChain() {
    when(chatClientProvider.create(any(), any(ChatClientProfile.class), any()))
        .thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
    when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
    lenient().when(requestSpec.tools(any())).thenReturn(requestSpec);
    lenient().when(requestSpec.tools(any(), any())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("ok");
  }

  private void stubCallChainWithoutTools() {
    when(chatClientProvider.create(any(), any(ChatClientProfile.class), any()))
        .thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
    when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("ok");
  }
}
