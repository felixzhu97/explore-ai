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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
        // Tool-calling models (e.g. DeepSeek) often stream DSML/tool_calls markup as
        // plain text on .stream().content(). Use blocking call so ToolCallingAdvisor
        // completes the tool loop, then emit the sanitized final answer.
        if (usesTools(agent)) {
            return Mono.fromCallable(() -> invoke(agent, task))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(answer -> {
                        if (answer == null || answer.isBlank()) {
                            return Flux.empty();
                        }
                        return Flux.just(answer);
                    });
        }
        return basePrompt(agent, task).stream()
                .content()
                .map(ToolCallMarkupFilter::sanitize)
                .filter(chunk -> chunk != null && !chunk.isEmpty());
    }

    @Override
    public String invoke(AgentDefinition agent, String task) {
        String content = basePrompt(agent, task).call().content();
        return ToolCallMarkupFilter.sanitize(content == null ? "" : content);
    }

    private boolean usesTools(AgentDefinition agent) {
        return switch (agent.type().value()) {
            case "vectordb", "research", "weather" -> true;
            default -> false;
        };
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
