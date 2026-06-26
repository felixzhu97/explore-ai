package com.ai.common.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Service for handling Server-Sent Events (SSE) streaming responses.
 */
@Service
public class StreamingService {

    private static final Duration DEFAULT_WORD_DELAY = Duration.ofMillis(30);
    private final ObjectMapper objectMapper;

    public StreamingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Flux<ServerSentEvent<String>> streamWords(String text) {
        return streamWords(text, DEFAULT_WORD_DELAY);
    }

    public Flux<ServerSentEvent<String>> streamWords(String text, Duration delayPerWord) {
        if (text == null || text.isEmpty()) {
            return Flux.just(ServerSentEvent.<String>builder().data("").build());
        }
        String[] words = text.split(" ");
        return Flux.fromArray(words)
                .delayElements(delayPerWord)
                .map(word -> ServerSentEvent.<String>builder().data(word + " ").build());
    }

    public Flux<ServerSentEvent<String>> streamWithSources(String text, String sourcesJson) {
        Flux<ServerSentEvent<String>> wordStream = streamWords(text);
        Flux<ServerSentEvent<String>> sourceEvent = Flux.defer(() -> {
            if (sourcesJson == null || sourcesJson.isEmpty()) return Flux.empty();
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("sources").data(sourcesJson).build());
        });
        return wordStream.concatWith(sourceEvent);
    }

    public <T> Flux<ServerSentEvent<String>> streamWithSources(String text, T sources) {
        try {
            return streamWithSources(text, objectMapper.writeValueAsString(sources));
        } catch (JsonProcessingException e) {
            return streamWords(text);
        }
    }

    @FunctionalInterface
    public interface SourcesSerializer {
        String serialize() throws JsonProcessingException;
    }

    public Flux<ServerSentEvent<String>> streamWithSources(String text, SourcesSerializer sourcesSerializer) {
        try {
            return streamWithSources(text, sourcesSerializer.serialize());
        } catch (JsonProcessingException e) {
            return streamWords(text);
        }
    }
}
