package com.ai.chat.infrastructure.llm;

import com.ai.chat.application.usecase.TextChatOptions;
import com.ai.common.domain.port.out.DocumentSearchTool;
import com.ai.common.domain.port.out.WebSearchTool;
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
public class ChatClientFactory {

    private final ChatModelResolver chatModelResolver;
    private final ChatMemory chatMemory;
    private final WeatherTools weatherTools;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;
    private final boolean loggingAdvisorEnabled;

    public ChatClientFactory(
            ChatModelResolver chatModelResolver,
            ChatMemory chatMemory,
            WeatherTools weatherTools,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool,
            @Value("${app.ai.logging-advisor.enabled:true}") boolean loggingAdvisorEnabled) {
        this.chatModelResolver = chatModelResolver;
        this.chatMemory = chatMemory;
        this.weatherTools = weatherTools;
        this.documentSearchTool = documentSearchTool;
        this.webSearchTool = webSearchTool;
        this.loggingAdvisorEnabled = loggingAdvisorEnabled;
    }

    public ChatClient create(TextChatOptions options) {
        return buildClient(options, true);
    }

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
                .defaultAdvisors(advisors);

        if (options.toolsEnabled()) {
            builder.defaultTools(weatherTools, documentSearchTool, webSearchTool);
        }

        return builder.build();
    }
}
