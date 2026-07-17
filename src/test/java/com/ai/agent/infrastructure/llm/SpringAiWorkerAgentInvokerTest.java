package com.ai.agent.infrastructure.llm;

import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.vo.AgentType;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiWorkerAgentInvokerTest {

    @Mock
    private ChatClientProvider chatClientProvider;
    @Mock
    private DocumentSearchTool documentSearchTool;
    @Mock
    private WebSearchTool webSearchTool;
    @Mock
    private WeatherTool weatherTool;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Test
    void should_bind_weather_tool_when_weather_agent_invoked() {
        SpringAiWorkerAgentInvoker invoker = newInvoker();
        stubCallChain();

        invoker.invoke(agent("weather"), "Beijing weather");

        ArgumentCaptor<Object[]> tools = ArgumentCaptor.forClass(Object[].class);
        verify(requestSpec).tools(tools.capture());
        assertSame(weatherTool, tools.getValue()[0]);
    }

    @Test
    void should_bind_web_search_tool_when_research_agent_invoked() {
        SpringAiWorkerAgentInvoker invoker = newInvoker();
        stubCallChain();

        invoker.invoke(agent("research"), "latest Spring AI release");

        ArgumentCaptor<Object[]> tools = ArgumentCaptor.forClass(Object[].class);
        verify(requestSpec).tools(tools.capture());
        assertSame(webSearchTool, tools.getValue()[0]);
    }

    @Test
    void should_bind_document_search_tool_when_vectordb_agent_invoked() {
        SpringAiWorkerAgentInvoker invoker = newInvoker();
        stubCallChain();

        invoker.invoke(agent("vectordb"), "find onboarding docs");

        ArgumentCaptor<Object[]> tools = ArgumentCaptor.forClass(Object[].class);
        verify(requestSpec).tools(tools.capture());
        assertSame(documentSearchTool, tools.getValue()[0]);
    }

    @Test
    void should_skip_tools_when_analyst_agent_invoked() {
        SpringAiWorkerAgentInvoker invoker = newInvoker();
        stubCallChainWithoutTools();

        invoker.invoke(agent("analyst"), "summarize findings");

        verify(requestSpec, never()).tools(any());
        verify(requestSpec).call();
    }

    @Test
    void should_use_blocking_call_when_streaming_tool_agent() {
        SpringAiWorkerAgentInvoker invoker = newInvoker();
        stubCallChain();
        when(callResponseSpec.content()).thenReturn(
                "brief <｜DSML｜tool_calls>leak</｜DSML｜tool_calls> done");

        StepVerifier.create(invoker.invokeStream(agent("research"), "search topic"))
                .expectNext("brief  done")
                .verifyComplete();

        verify(requestSpec).call();
        verify(requestSpec, never()).stream();
    }

    private SpringAiWorkerAgentInvoker newInvoker() {
        return new SpringAiWorkerAgentInvoker(
                chatClientProvider,
                documentSearchTool,
                webSearchTool,
                weatherTool);
    }

    private static AgentDefinition agent(String type) {
        return AgentDefinition.create(AgentType.of(type), type, type, "system");
    }

    private void stubCallChain() {
        when(chatClientProvider.createBareStateless(any())).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("ok");
    }

    private void stubCallChainWithoutTools() {
        when(chatClientProvider.createBareStateless(any())).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("ok");
    }
}
