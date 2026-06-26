package com.ai.ai.infrastructure.streaming;

import com.ai.ai.infrastructure.streaming.StreamingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StreamingService")
class StreamingServiceTest {

    private StreamingService streamingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        streamingService = new StreamingService(objectMapper);
    }

    @Nested
    @DisplayName("streamWords(String)")
    class StreamWords {

        @Test
        @DisplayName("should return empty event when text is null")
        void shouldReturnEmptyEventWhenTextIsNull() {
            Flux<ServerSentEvent<String>> result = streamingService.streamWords(null);

            StepVerifier.create(result)
                    .expectNextMatches(event -> event.data() != null && event.data().isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty event when text is empty")
        void shouldReturnEmptyEventWhenTextIsEmpty() {
            Flux<ServerSentEvent<String>> result = streamingService.streamWords("");

            StepVerifier.create(result)
                    .expectNextMatches(event -> event.data() != null && event.data().isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return words stream when text has content")
        void shouldReturnWordsStreamWhenTextHasContent() {
            String text = "Hello World";
            Flux<ServerSentEvent<String>> result = streamingService.streamWords(text);

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Hello ".equals(event.data()))
                    .expectNextMatches(event -> "World ".equals(event.data()))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("streamWords(String, Duration)")
    class StreamWordsWithDelay {

        @Test
        @DisplayName("should use custom delay between words")
        void shouldUseCustomDelayBetweenWords() {
            String text = "A B";
            Duration customDelay = Duration.ofMillis(10);
            Flux<ServerSentEvent<String>> result = streamingService.streamWords(text, customDelay);

            long startTime = System.currentTimeMillis();

            StepVerifier.create(result)
                    .expectNextMatches(event -> "A ".equals(event.data()))
                    .expectNextMatches(event -> "B ".equals(event.data()))
                    .verifyComplete();

            long elapsed = System.currentTimeMillis() - startTime;
            assertThat(elapsed).isGreaterThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("streamWithSources(String, String)")
    class StreamWithSourcesJson {

        @Test
        @DisplayName("should return text stream without sources when sourcesJson is null")
        void shouldReturnTextStreamWithoutSourcesWhenSourcesJsonIsNull() {
            String text = "Hello World";
            Flux<ServerSentEvent<String>> result = streamingService.streamWithSources(text, (String) null);

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Hello ".equals(event.data()))
                    .expectNextMatches(event -> "World ".equals(event.data()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return text stream without sources when sourcesJson is empty")
        void shouldReturnTextStreamWithoutSourcesWhenSourcesJsonIsEmpty() {
            String text = "Hello";
            Flux<ServerSentEvent<String>> result = streamingService.streamWithSources(text, "");

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Hello ".equals(event.data()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return text stream followed by sources event")
        void shouldReturnTextStreamFollowedBySourcesEvent() {
            String text = "Hello";
            String sourcesJson = "{\"sources\":[\"doc1\"]}";
            Flux<ServerSentEvent<String>> result = streamingService.streamWithSources(text, sourcesJson);

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Hello ".equals(event.data()))
                    .expectNextMatches(event -> "sources".equals(event.event()) && sourcesJson.equals(event.data()))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("streamWithSources(String, Object)")
    class StreamWithSourcesObject {

        @Test
        @DisplayName("should serialize object to JSON and send as sources event")
        void shouldSerializeObjectToJsonAndSendAsSourcesEvent() {
            String text = "Test";
            SourcesDto sources = new SourcesDto("source1", 0.95);

            Flux<ServerSentEvent<String>> result = streamingService.streamWithSources(text, sources);

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Test ".equals(event.data()))
                    .expectNextMatches(event -> {
                        String data = event.data();
                        return data != null && data.contains("\"source\":\"source1\"") && data.contains("0.95");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return original text stream when serialization fails")
        void shouldReturnOriginalTextStreamWhenSerializationFails() {
            String text = "Hello";
            Object badObject = new UnserializableObject();

            Flux<ServerSentEvent<String>> result = streamingService.streamWithSources(text, badObject);

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Hello ".equals(event.data()))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("streamWithSources(String, SourcesSerializer)")
    class StreamWithSourcesSerializer {

        @Test
        @DisplayName("should use custom serializer to generate sources")
        void shouldUseCustomSerializerToGenerateSources() {
            String text = "Hello";
            String expectedJson = "{\"custom\":\"data\"}";
            StreamingService.SourcesSerializer serializer = () -> expectedJson;

            Flux<ServerSentEvent<String>> result = streamingService.streamWithSources(text, serializer);

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Hello ".equals(event.data()))
                    .expectNextMatches(event -> expectedJson.equals(event.data()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return original text stream when serializer throws exception")
        void shouldReturnOriginalTextStreamWhenSerializerThrowsException() {
            String text = "Test";
            StreamingService.SourcesSerializer failingSerializer = () -> {
                throw new JsonProcessingException("Serialization failed") {};
            };

            Flux<ServerSentEvent<String>> result = streamingService.streamWithSources(text, failingSerializer);

            StepVerifier.create(result)
                    .expectNextMatches(event -> "Test ".equals(event.data()))
                    .verifyComplete();
        }
    }

    // Helper classes for testing
    record SourcesDto(String source, double score) {}

    static class UnserializableObject {
        private final Object circularRef = this;

        @Override
        public String toString() {
            return "UnserializableObject{circularRef=" + circularRef + "}";
        }
    }
}
