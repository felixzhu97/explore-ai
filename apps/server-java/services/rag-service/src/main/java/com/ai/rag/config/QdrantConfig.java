package com.ai.rag.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

	@Value("${rag.qdrant.host:localhost}")
	private String host;

	@Value("${rag.qdrant.port:6333}")
	private Integer port;

	@Bean
	public QdrantClient qdrantClient() {
		return new QdrantClient(
				QdrantGrpcClient.newBuilder(host, port, false).build()
		);
	}
}
