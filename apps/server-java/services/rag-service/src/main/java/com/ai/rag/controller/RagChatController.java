package com.ai.rag.controller;

import com.ai.rag.model.RagChatRequest;
import com.ai.rag.model.SourceDocument;
import com.ai.rag.service.RagChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rag")
public class RagChatController {

	private final RagChatService ragChatService;

	public RagChatController(RagChatService ragChatService) {
		this.ragChatService = ragChatService;
	}

	@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody RagChatRequest request) {
		List<SourceDocument> sources = ragChatService.searchSources(request.query(), request.topK());
		String sourcesJson = sources.stream()
				.map(s -> "{\"text\":\"" + escapeJson(s.text()) + "\",\"score\":" + s.score() + "}")
				.collect(Collectors.joining(",", "[", "]"));

		Flux<ServerSentEvent<String>> sourceEvent = Flux.just(
				ServerSentEvent.<String>builder()
						.event("sources")
						.data(sourcesJson)
						.build()
		);

		Flux<ServerSentEvent<String>> contentEvents = ragChatService.streamChat(request)
				.map(chunk -> ServerSentEvent.<String>builder()
						.data(chunk)
						.build());

		Flux<ServerSentEvent<String>> doneEvent = Flux.just(
				ServerSentEvent.<String>builder()
						.data("[DONE]")
						.build()
		);

		return Flux.concat(sourceEvent, contentEvents, doneEvent);
	}

	private String escapeJson(String text) {
		if (text == null) return "";
		return text
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}
