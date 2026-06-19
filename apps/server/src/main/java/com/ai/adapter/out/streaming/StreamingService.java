package com.ai.adapter.out.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Service for handling Server-Sent Events (SSE) streaming responses.
 * Provides reusable streaming utilities across different controllers.
 */
@Service
public class StreamingService {

    private static final Logger log = LoggerFactory.getLogger(StreamingService.class);
    private static final Duration DEFAULT_WORD_DELAY = Duration.ofMillis(30);

    private final ObjectMapper objectMapper;

    public StreamingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a stream of words from the given text.
     *
     * @param text The text to stream
     * @return Flux of SSE events containing words
     */
    public Flux<ServerSentEvent<String>> streamWords(String text) {
        return streamWords(text, DEFAULT_WORD_DELAY);
    }

    /**
     * Creates a stream of words from the given text with custom delay.
     *
     * @param text The text to stream
     * @param delayPerWord Delay between each word
     * @return Flux of SSE events containing words
     */
    public Flux<ServerSentEvent<String>> streamWords(String text, Duration delayPerWord) {
        if (text == null || text.isEmpty()) {
            return Flux.just(ServerSentEvent.<String>builder().data("").build());
        }

        String[] words = text.split(" ");
        return Flux.fromArray(words)
                .delayElements(delayPerWord)
                .map(word -> ServerSentEvent.<String>builder().data(word + " ").build());
    }

    /**
     * Creates a stream that sends text words followed by a final event with sources.
     *
     * @param text The main text to stream
     * @param sourcesJson JSON string of sources to send as final event
     * @return Flux of SSE events
     */
    public Flux<ServerSentEvent<String>> streamWithSources(String text, String sourcesJson) {
        Flux<ServerSentEvent<String>> wordStream = streamWords(text);

        Flux<ServerSentEvent<String>> sourceEvent = Flux.defer(() -> {
            if (sourcesJson == null || sourcesJson.isEmpty()) {
                return Flux.empty();
            }
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("sources")
                    .data(sourcesJson)
                    .build());
        });

        return wordStream.concatWith(sourceEvent);
    }

    /**
     * Creates a stream that sends text words followed by a final event with sources.
     *
     * @param text The main text to stream
     * @param sources The sources object to serialize and send as final event
     * @param <T> The type of the sources object
     * @return Flux of SSE events
     */
    public <T> Flux<ServerSentEvent<String>> streamWithSources(String text, T sources) {
        try {
            String sourcesJson = objectMapper.writeValueAsString(sources);
            return streamWithSources(text, sourcesJson);
        } catch (JsonProcessingException e) {
            log.error("Error serializing sources", e);
            return streamWords(text);
        }
    }

    /**
     * Creates a stream that sends text words followed by a final event with sources.
     *
     * @param text The main text to stream
     * @param sourcesSerializer Function to serialize sources to JSON
     * @return Flux of SSE events
     */
    public Flux<ServerSentEvent<String>> streamWithSources(String text, SourcesSerializer sourcesSerializer) {
        try {
            String sourcesJson = sourcesSerializer.serialize();
            return streamWithSources(text, sourcesJson);
        } catch (JsonProcessingException e) {
            log.error("Error serializing sources", e);
            return streamWords(text);
        }
    }

    /**
     * Functional interface for serializing sources to JSON.
     */
    @FunctionalInterface
    public interface SourcesSerializer {
        String serialize() throws JsonProcessingException;
    }
}
