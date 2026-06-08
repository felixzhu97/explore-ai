package com.ai.rag.config;

import dev.langchain4j.embedding.EmbeddingModel;
import dev.langchain4j.embedding.onnx.HuggingFaceEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

	@Value("${rag.qdrant.embedding-model-name:sentence-transformers/all-MiniLM-L6-v2}")
	private String embeddingModelName;

	@Bean
	public EmbeddingModel embeddingModel() {
		return HuggingFaceEmbeddingModel.builder()
				.modelName(embeddingModelName)
				.build();
	}
}
