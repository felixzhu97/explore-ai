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
import org.springframework.ai.model.tool.ToolCallingManager;
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
    private final boolean loggingAdvisorEnabled;

    public ChatClientFactory(
            ChatModelResolver chatModelResolver,
            ChatMemory chatMemory,
            PromptTemplates promptTemplates,
            WeatherTool weatherTools,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool,
            @Value("${app.ai.logging-advisor.enabled:true}") boolean loggingAdvisorEnabled) {
        this.chatModelResolver = chatModelResolver;
        this.chatMemory = chatMemory;
        this.promptTemplates = promptTemplates;
        this.weatherTools = weatherTools;
        this.documentSearchTool = documentSearchTool;
        this.webSearchTool = webSearchTool;
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
        // ToolAdvisor in the chain skips ChatClient's auto-registered ToolCallingAdvisor.
        advisors.add(AnswerAfterToolsAdvisor.builder()
                .toolCallingManager(ToolCallingManager.builder().build())
                .build());

        ChatClient.Builder builder = ChatClient.builder(resolved.chatModel())
                .defaultOptions(resolved.optionsBuilder())
                .defaultAdvisors(advisors);

        if (withDefaults) {
            builder.defaultSystem(promptTemplates.getDefaultSystemPrompt());
            if (options.toolsEnabled()) {
                builder.defaultTools(weatherTools, documentSearchTool, webSearchTool);
            }
        }

        return builder.build();
    }
}
