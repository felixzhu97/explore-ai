package com.ai.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    
    private final ChatModel chatModel;
    private final SystemPromptTemplate systemPrompt;
    
    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.systemPrompt = new SystemPromptTemplate(
            "You are a helpful AI assistant. Respond in a friendly and concise manner."
        );
    }
    
    public String chat(String userMessage) {
        Prompt prompt = this.systemPrompt.create(
            new org.springframework.ai.chat.messages.UserMessage(userMessage)
        );
        
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}
