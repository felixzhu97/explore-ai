package com.ai.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

	@Value("${rag.llm.provider:openai}")
	private String provider;

	@Value("${rag.llm.model-name:gpt-4o}")
	private String modelName;

	@Value("${rag.llm.api-key:}")
	private String apiKey;

	@Value("${rag.llm.base-url:}")
	private String baseUrl;

	@Bean("ragChatClient")
	public ChatClient ragChatClient(ChatClient.Builder builder) {
		return builder.build();
	}
}
