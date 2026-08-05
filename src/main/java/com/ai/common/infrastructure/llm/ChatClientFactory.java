package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.ChatClientProfile;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.repository.DateTimeTool;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ChatClientFactory implements ChatClientProvider {

    private final ChatModelResolver chatModelResolver;
    private final ChatMemory chatMemory;
    private final PromptTemplates promptTemplates;
    private final ObjectProvider<ToolCallback[]> mcpToolCallbacks;
    private final boolean loggingAdvisorEnabled;
    private final boolean toolSearchEnabled;
    private final ToolCallback[] localToolCallbacks;
    private final RegexToolIndex toolSearchIndex;

    public ChatClientFactory(
            ChatModelResolver chatModelResolver,
            ChatMemory chatMemory,
            PromptTemplates promptTemplates,
            WeatherTool weatherTools,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool,
            DateTimeTool dateTimeTool,
            ObjectProvider<ToolCallback[]> mcpToolCallbacks,
            @Value("${app.ai.logging-advisor.enabled:true}") boolean loggingAdvisorEnabled,
            @Value("${app.ai.tool-search.enabled:false}") boolean toolSearchEnabled) {
        this.chatModelResolver = chatModelResolver;
        this.chatMemory = chatMemory;
        this.promptTemplates = promptTemplates;
        this.mcpToolCallbacks = mcpToolCallbacks;
        this.loggingAdvisorEnabled = loggingAdvisorEnabled;
        this.toolSearchEnabled = toolSearchEnabled;
        this.toolSearchIndex = new RegexToolIndex();
        this.localToolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools, documentSearchTool, webSearchTool, dateTimeTool)
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
            advisors.add(createToolCallingAdvisor());
        }

        ChatClient.Builder builder = ChatClient.builder(resolved.chatModel())
                .defaultOptions(resolved.optionsBuilder())
                .defaultAdvisors(advisors);

        if (withDefaults) {
            String systemPrompt = promptTemplates.getDefaultSystemPrompt();
            if (options.skillSystemPrompt() != null && !options.skillSystemPrompt().isBlank()) {
                systemPrompt = systemPrompt + "\n\n" + options.skillSystemPrompt();
            }
            builder.defaultSystem(systemPrompt);
            if (withTools) {
                ToolCallback[] callbacks = notifyingCallbacks(channelId);
                if (callbacks.length > 0) {
                    builder.defaultToolCallbacks(callbacks);
                }
            }
        }

        return builder.build();
    }

    /**
     * When {@code app.ai.tool-search.enabled=true}, expose tools via
     * {@link ToolSearchToolCallingAdvisor} so only search-matched tools enter the model
     * context. Otherwise keep {@link ToolCallLoopGuardAdvisor}.
     */
    Advisor createToolCallingAdvisor() {
        ToolCallingManager manager = ToolCallingManager.builder().build();
        if (toolSearchEnabled) {
            return ToolSearchToolCallingAdvisor.builder()
                    .toolCallingManager(manager)
                    .toolIndex(toolSearchIndex)
                    .build();
        }
        return ToolCallLoopGuardAdvisor.builder()
                .toolCallingManager(manager)
                .build();
    }

    boolean isToolSearchEnabled() {
        return toolSearchEnabled;
    }

    private ToolCallback[] notifyingCallbacks(String channelId) {
        String id = channelId == null ? "" : channelId;
        List<ToolCallback> callbacks = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (ToolCallback callback : localToolCallbacks) {
            String name = callback.getToolDefinition().name();
            if (names.add(name)) {
                callbacks.add(new NotifyingToolCallback(callback, id));
            }
        }
        // Prefer local @Tool over MCP when names collide (DeepSeek/Spring AI reject duplicates).
        ToolCallback[] mcp = mcpToolCallbacks.getIfAvailable();
        if (mcp != null) {
            for (ToolCallback callback : mcp) {
                String name = callback.getToolDefinition().name();
                if (names.add(name)) {
                    callbacks.add(new NotifyingToolCallback(callback, id));
                }
            }
        }
        return callbacks.toArray(ToolCallback[]::new);
    }
}
