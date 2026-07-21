package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.ChatClientProfile;
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
import org.springframework.ai.model.tool.ToolCallingManager;
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
    private final ObjectProvider<ToolCallback[]> mcpToolCallbacks;
    private final boolean loggingAdvisorEnabled;
    private final ToolCallback[] localToolCallbacks;

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
        this.mcpToolCallbacks = mcpToolCallbacks;
        this.loggingAdvisorEnabled = loggingAdvisorEnabled;
        this.localToolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools, documentSearchTool, webSearchTool)
                .build()
                .getToolCallbacks();
    }

    @Override
    public ChatClient create(TextChatOptions options) {
        return create(options, null);
    }

    @Override
    public ChatClient create(TextChatOptions options, String conversationId) {
        return create(options, ChatClientProfile.MEMORY_TOOLS, conversationId);
    }

    @Override
    public ChatClient createStateless(TextChatOptions options) {
        return create(options, ChatClientProfile.TOOLS, ToolEventChannel.getCurrentSessionId());
    }

    @Override
    public ChatClient createBareStateless(TextChatOptions options) {
        return create(options, ChatClientProfile.BARE, null);
    }

    @Override
    public ChatClient create(TextChatOptions options, ChatClientProfile profile, String conversationId) {
        ChatClientProfile effective = profile == null ? ChatClientProfile.MEMORY_TOOLS : profile;
        boolean withMemory = effective == ChatClientProfile.MEMORY_TOOLS || effective == ChatClientProfile.MEMORY;
        boolean withDefaults = effective != ChatClientProfile.BARE;
        boolean withTools = (effective == ChatClientProfile.MEMORY_TOOLS || effective == ChatClientProfile.TOOLS)
                && options.toolsEnabled();
        return buildClient(options, withMemory, withDefaults, withTools, conversationId);
    }

    private ChatClient buildClient(
            TextChatOptions options,
            boolean withMemory,
            boolean withDefaults,
            boolean withTools,
            String channelId) {
        ResolvedChatModel resolved = chatModelResolver.resolve(options);
        List<Advisor> advisors = new ArrayList<>();
        if (withMemory) {
            advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        if (loggingAdvisorEnabled) {
            advisors.add(SimpleLoggerAdvisor.builder().build());
        }
        if (withTools) {
            // ToolAdvisor in the chain skips ChatClient's auto-registered ToolCallingAdvisor.
            advisors.add(AnswerAfterToolsAdvisor.builder()
                    .toolCallingManager(ToolCallingManager.builder().build())
                    .build());
        }

        ChatClient.Builder builder = ChatClient.builder(resolved.chatModel())
                .defaultOptions(resolved.optionsBuilder())
                .defaultAdvisors(advisors);

        if (withDefaults) {
            builder.defaultSystem(promptTemplates.getDefaultSystemPrompt());
            if (withTools) {
                ToolCallback[] callbacks = notifyingCallbacks(channelId);
                if (callbacks.length > 0) {
                    builder.defaultToolCallbacks(callbacks);
                }
            }
        }

        return builder.build();
    }

    private ToolCallback[] notifyingCallbacks(String channelId) {
        String id = channelId == null ? "" : channelId;
        List<ToolCallback> callbacks = new ArrayList<>();
        for (ToolCallback callback : localToolCallbacks) {
            callbacks.add(new NotifyingToolCallback(callback, id));
        }
        ToolCallback[] mcp = mcpToolCallbacks.getIfAvailable();
        if (mcp != null) {
            for (ToolCallback callback : mcp) {
                callbacks.add(new NotifyingToolCallback(callback, id));
            }
        }
        return callbacks.toArray(ToolCallback[]::new);
    }
}
