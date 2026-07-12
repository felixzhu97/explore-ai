package com.ai.common.infrastructure.llm;

import com.ai.chat.application.usecase.ChatClientProvider;
import com.ai.chat.application.usecase.TextChatOptions;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.tools.infrastructure.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatClientFactory implements ChatClientProvider {

    private final ChatModelResolver chatModelResolver;
    private final ChatMemory chatMemory;
    private final PromptTemplates promptTemplates;
    private final WeatherTools weatherTools;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;
    private final boolean loggingAdvisorEnabled;

    public ChatClientFactory(
            ChatModelResolver chatModelResolver,
            ChatMemory chatMemory,
            PromptTemplates promptTemplates,
            WeatherTools weatherTools,
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
        return buildClient(options, conversationId != null);
    }

    @Override
    public ChatClient createStateless(TextChatOptions options) {
        return buildClient(options, false);
    }

    private ChatClient buildClient(TextChatOptions options, boolean withMemory) {
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
                .defaultSystem(promptTemplates.getDefaultSystemPrompt())
                .defaultAdvisors(advisors);

        if (options.toolsEnabled()) {
            builder.defaultTools(weatherTools, documentSearchTool, webSearchTool);
        }

        return builder.build();
    }
}
