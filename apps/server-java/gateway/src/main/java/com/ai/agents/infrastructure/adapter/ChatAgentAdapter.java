package com.ai.agents.infrastructure.adapter;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Chat Agent Adapter.
 * Default chat agent for general conversational interactions.
 */
@Component
public class ChatAgentAdapter implements AgentAdapter {

    private static final Logger log = LoggerFactory.getLogger(ChatAgentAdapter.class);

    private final ChatModel chatModel;

    @Value("${ai.agents.chat.system-prompt:You are a helpful and friendly AI assistant.}")
    private String systemPrompt;

    public ChatAgentAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AgentType getType() {
        return AgentType.CHAT;
    }

    @Override
    public Mono<AgentResponseDto> execute(Conversation conversation, AgentRequestDto request) {
        log.info("Chat agent processing request: {}", truncate(request.getUserMessage(), 50));

        return Mono.fromCallable(() -> {
            try {
                String userMessage = request.getUserMessage();
                dev.langchain4j.data.message.ChatMessage systemMsg = SystemMessage.from(systemPrompt);
                dev.langchain4j.data.message.ChatMessage userMsg = UserMessage.from(userMessage);

                ChatResponse response = chatModel.chat(List.of(systemMsg, userMsg));
                String answer = response.aiMessage().text();

                return AgentResponseDto.success(answer, AgentType.CHAT);
            } catch (Exception e) {
                log.error("Chat processing failed", e);
                return AgentResponseDto.error("Chat failed: " + e.getMessage());
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    @Override
    public boolean isAvailable() {
        return chatModel != null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
