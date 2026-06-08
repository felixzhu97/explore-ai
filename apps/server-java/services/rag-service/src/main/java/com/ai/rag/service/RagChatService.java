package com.ai.rag.service;

import com.ai.rag.model.RagChatRequest;
import com.ai.rag.model.SourceDocument;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class RagChatService {

	private final VectorSearchService vectorSearchService;
	private final ChatLanguageModel chatModel;

	public RagChatService(
			VectorSearchService vectorSearchService,
			@Qualifier("chatLanguageModel") ChatLanguageModel chatModel
	) {
		this.vectorSearchService = vectorSearchService;
		this.chatModel = chatModel;
	}

	public Flux<String> streamChat(RagChatRequest request) {
		List<String> contextChunks = vectorSearchService.searchSimilar(request.query(), request.topK());
		String context = String.join("\n\n", contextChunks);

		String prompt = String.format("""
				Use the following context to answer the user's question.
				If you cannot find the answer in the context, say "I don't have enough information."
				Keep your answer concise and relevant.

				Context:
				%s

				Question:
				%s
				""", context, request.query());

		AiMessage response = chatModel.chat(UserMessage.from(prompt));

		return Flux.just(response.text());
	}

	public List<SourceDocument> searchSources(String query, int topK) {
		return vectorSearchService.searchSimilar(query, topK).stream()
				.map(text -> new SourceDocument(text, 0.95))
				.toList();
	}
}
