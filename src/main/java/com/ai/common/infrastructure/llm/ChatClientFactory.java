package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatClientFactory implements ChatClientProvider {

    private final ChatModelResolver chatModelResolver;
    private final ChatMemory chatMemory;
    private final PromptTemplates promptTemplates;
    private final WeatherTool weatherTools;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;
    private final ObjectProvider<ToolCallback[]> mcpToolCallbacks;
    private final boolean loggingAdvisorEnabled;

    public ChatClientFactory(
            ChatModelResolver chatModelResolver,
            ChatMemory chatMemory,
            PromptTemplates promptTemplates,
            WeatherTool weatherTools,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool,
            ObjectProvider<ToolCallback[]> mcpToolCallbacks,
            @Value("${app.ai.logging-advisor.enabled:true}") boolean loggingAdvisorEnabled) {
        this.chatModelResolver = chatModelResolver;
        this.chatMemory = chatMemory;
        this.promptTemplates = promptTemplates;
        this.weatherTools = weatherTools;
        this.documentSearchTool = documentSearchTool;
        this.webSearchTool = webSearchTool;
        this.mcpToolCallbacks = mcpToolCallbacks;
        this.loggingAdvisorEnabled = loggingAdvisorEnabled;
    }

    @Override
    public ChatClient create(TextChatOptions options) {
        return create(options, null);
    }

    @Override
    public ChatClient create(TextChatOptions options, String conversationId) {
        return buildClient(options, conversationId != null, true);
    }

    @Override
    public ChatClient createStateless(TextChatOptions options) {
        return buildClient(options, false, true);
    }

    @Override
    public ChatClient createBareStateless(TextChatOptions options) {
        return buildClient(options, false, false);
    }

    private ChatClient buildClient(TextChatOptions options, boolean withMemory, boolean withDefaults) {
        ResolvedChatModel resolved = chatModelResolver.resolve(options);
        List<Advisor> advisors = new ArrayList<>();
        if (withMemory) {
            advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        if (loggingAdvisorEnabled) {
            advisors.add(SimpleLoggerAdvisor.builder().build());
        }

        ChatClient.Builder builder = ChatClient.builder(resolved.chatModel())
                .defaultOptions(resolved.optionsBuilder())
                .defaultAdvisors(advisors);

        if (withDefaults) {
            builder.defaultSystem(promptTemplates.getDefaultSystemPrompt());
            if (options.toolsEnabled()) {
                ToolCallback[] callbacks = notifyingCallbacks();
                if (callbacks.length > 0) {
                    builder.defaultToolCallbacks(callbacks);
                }
            }
        }

        return builder.build();
    }

    private ToolCallback[] notifyingCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        ToolCallback[] local = MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools, documentSearchTool, webSearchTool)
                .build()
                .getToolCallbacks();
        for (ToolCallback callback : local) {
            callbacks.add(new NotifyingToolCallback(callback));
        }
        ToolCallback[] mcp = mcpToolCallbacks.getIfAvailable();
        if (mcp != null) {
            for (ToolCallback callback : mcp) {
                callbacks.add(new NotifyingToolCallback(callback));
            }
        }
        return callbacks.toArray(ToolCallback[]::new);
    }
}
