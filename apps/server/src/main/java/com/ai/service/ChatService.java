package com.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatModel chatModel;
    
    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    public String chat(String userMessage) {
        log.info("Chat request: {}", userMessage);
        UserMessage userMsg = new UserMessage(userMessage);
        Prompt prompt = new Prompt(userMsg);
        
        try {
            ChatResponse response = chatModel.call(prompt);
            String text = response.getResult().getOutput().getText();
            log.info("Chat response: {}", text);
            return text;
        } catch (Exception e) {
            log.error("Chat error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }
}
