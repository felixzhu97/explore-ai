package com.ai.common.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Service for handling Server-Sent Events (SSE) streaming responses. */
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

  /** Emits the text word by word as SSE events, pausing {@code delayPerWord} between words. */
  public Flux<ServerSentEvent<String>> streamWords(String text, Duration delayPerWord) {
    if (text == null || text.isEmpty()) {
      return Flux.just(ServerSentEvent.<String>builder().data("").build());
    }
    String[] words = text.split(" ");
    return Flux.fromArray(words)
        .delayElements(delayPerWord)
        .map(word -> ServerSentEvent.<String>builder().data(word + " ").build());
  }

  /** Streams the text word by word, then a {@code sources} event when JSON is present. */
  public Flux<ServerSentEvent<String>> streamWithSources(String text, String sourcesJson) {
    Flux<ServerSentEvent<String>> wordStream = streamWords(text);
    Flux<ServerSentEvent<String>> sourceEvent =
        Flux.defer(
            () -> {
              if (sourcesJson == null || sourcesJson.isEmpty()) {
                return Flux.empty();
              }
              return Flux.just(
                  ServerSentEvent.<String>builder().event("sources").data(sourcesJson).build());
            });
    return wordStream.concatWith(sourceEvent);
  }

  /** Serializes the sources to JSON and streams them after the text; omits them on failure. */
  public <T> Flux<ServerSentEvent<String>> streamWithSources(String text, T sources) {
    try {
      return streamWithSources(text, objectMapper.writeValueAsString(sources));
    } catch (JsonProcessingException e) {
      return streamWords(text);
    }
  }

  /** Streams the text followed by the serializer's sources JSON; omits sources on failure. */
  public Flux<ServerSentEvent<String>> streamWithSources(
      String text, SourcesSerializer sourcesSerializer) {
    try {
      return streamWithSources(text, sourcesSerializer.serialize());
    } catch (JsonProcessingException e) {
      return streamWords(text);
    }
  }

  /** Callback that produces the sources JSON payload, possibly failing with a Jackson error. */
  @FunctionalInterface
  public interface SourcesSerializer {
    String serialize() throws JsonProcessingException;
  }
}
