package com.ai.rag.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.chat.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

	@Value("${rag.llm.provider:openai}")
	private String provider;

	@Value("${rag.llm.model-name:gpt-4o}")
	private String modelName;

	@Value("${rag.llm.api-key:}")
	private String apiKey;

	@Value("${rag.llm.base-url:}")
	private String baseUrl;

	@Bean
	public ChatLanguageModel chatLanguageModel() {
		if ("ollama".equalsIgnoreCase(provider)) {
			return OllamaChatModel.builder()
					.baseUrl(baseUrl.isBlank() ? "http://localhost:11434" : baseUrl)
					.modelName(modelName)
					.build();
		}

		return OpenAiChatModel.builder()
				.apiKey(apiKey.isBlank() ? "dummy-key" : apiKey)
				.modelName(modelName)
				.build();
	}
}
