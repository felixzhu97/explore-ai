package com.ai.pipeline.infra.llm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.infra.skills.AgentSkillsRuntime;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.DocumentSearchTool;
import com.ai.common.service.llm.WebSearchTool;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.tools.infra.tools.DateTimeTools;
import com.ai.tools.infra.tools.WeatherTools;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiWorkerAgentInvoker")
class SpringAiWorkerAgentInvokerTest {

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private DocumentSearchTool documentSearchTool;
  @Mock private WebSearchTool webSearchTool;
  @Mock private WeatherTools weatherTool;
  @Mock private DateTimeTools dateTimeTool;
  @Mock private AgentSkillsRuntime agentSkillsRuntime;
  @Mock private ChatClient chatClient;

  @Mock(answer = Answers.RETURNS_SELF)
  private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  private SpringAiWorkerAgentInvoker invoker;

  @BeforeEach
  void setUp() {
    invoker =
        new SpringAiWorkerAgentInvoker(
            chatClientProvider,
            documentSearchTool,
            webSearchTool,
            weatherTool,
            dateTimeTool,
            agentSkillsRuntime);
  }

  @Test
  @DisplayName("should bind the weather tool when the weather agent runs")
  void shouldBindTheWeatherToolWhenTheWeatherAgentRuns() {
    givenModelReplies("ok");

    invoker.invokeAgent(agent("weather", "weather"), "Beijing weather");

    verify(requestSpec).tools(weatherTool);
  }

  @Test
  @DisplayName("should bind web search and datetime tools when the research agent runs")
  void shouldBindWebSearchAndDatetimeToolsWhenTheResearchAgentRuns() {
    givenModelReplies("ok");

    invoker.invokeAgent(agent("research", "web", "datetime"), "latest Spring AI release");

    verify(requestSpec).tools(webSearchTool, dateTimeTool);
  }

  @Test
  @DisplayName("should bind the document search tool when the vectordb agent runs")
  void shouldBindTheDocumentSearchToolWhenTheVectordbAgentRuns() {
    givenModelReplies("ok");

    invoker.invokeAgent(agent("vectordb", "document"), "find onboarding docs");

    verify(requestSpec).tools(documentSearchTool);
  }

  @Test
  @DisplayName("should bind no tools when the agent declares none")
  void shouldBindNoToolsWhenTheAgentDeclaresNone() {
    givenModelReplies("ok");

    invoker.invokeAgent(agent("analyst"), "summarize findings");

    verify(requestSpec, never()).tools(any(Object[].class));
  }

  @Test
  @DisplayName("should stream a blocking reply without leaked tool markup for tool agents")
  void shouldStreamABlockingReplyWithoutLeakedToolMarkupForToolAgents() {
    givenModelReplies("brief <｜DSML｜tool_calls>leak</｜DSML｜tool_calls> done");

    StepVerifier.create(invoker.invokeStream(agent("research", "web", "datetime"), "search topic"))
        .expectNext("brief  done")
        .verifyComplete();

    verify(requestSpec, never()).stream();
  }

  private void givenModelReplies(String content) {
    when(agentSkillsRuntime.augmentSystemPrompt("system")).thenReturn("system");
    when(chatClientProvider.create(any(), any(ChatClientProfile.class), any()))
        .thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn(content);
  }

  private static AgentDefinition agent(String type, String... tools) {
    return AgentDefinition.createDefinition(
        AgentType.createType(type), type, type, "system", List.of(tools), "single");
  }
}
