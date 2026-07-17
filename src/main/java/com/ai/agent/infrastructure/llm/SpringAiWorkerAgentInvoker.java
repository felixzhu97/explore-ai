package com.ai.agent.infrastructure.llm;

import com.ai.agent.application.port.WorkerAgentInvoker;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class SpringAiWorkerAgentInvoker implements WorkerAgentInvoker {

    private final ChatClientProvider chatClientProvider;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;
    private final WeatherTool weatherTool;

    public SpringAiWorkerAgentInvoker(
            ChatClientProvider chatClientProvider,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool,
            WeatherTool weatherTool) {
        this.chatClientProvider = chatClientProvider;
        this.documentSearchTool = documentSearchTool;
        this.webSearchTool = webSearchTool;
        this.weatherTool = weatherTool;
    }

    @Override
    public Flux<String> invokeStream(AgentDefinition agent, String task) {
        ChatClient.ChatClientRequestSpec spec = basePrompt(agent, task);
        return spec.stream().content();
    }

    @Override
    public String invoke(AgentDefinition agent, String task) {
        String content = basePrompt(agent, task).call().content();
        return content == null ? "" : content;
    }

    private ChatClient.ChatClientRequestSpec basePrompt(AgentDefinition agent, String task) {
        ChatClient client = chatClientProvider.createBareStateless(TextChatOptions.defaults());
        ChatClient.ChatClientRequestSpec spec = client.prompt()
                .system(agent.systemPrompt())
                .user(task);

        return switch (agent.type().value()) {
            case "vectordb" -> spec.tools(documentSearchTool);
            case "research" -> spec.tools(webSearchTool);
            case "weather" -> spec.tools(weatherTool);
            default -> spec;
        };
    }
}
