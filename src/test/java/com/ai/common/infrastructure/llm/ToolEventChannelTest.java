package com.ai.common.infrastructure.llm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolEventChannel")
class ToolEventChannelTest {

    @AfterEach
    void tearDown() {
        ToolEventChannel.close("a");
        ToolEventChannel.close("b");
        ToolEventChannel.clearCurrentSessionId();
    }

    @Nested
    @DisplayName("isolation")
    class Isolation {

        @Test
        void should_not_cross_publish_between_channels() throws Exception {
            var sinkA = ToolEventChannel.open("a");
            ToolEventChannel.clearCurrentSessionId();
            var sinkB = ToolEventChannel.open("b");
            ToolEventChannel.clearCurrentSessionId();

            List<String> eventsA = new ArrayList<>();
            List<String> eventsB = new ArrayList<>();
            sinkA.asFlux().subscribe(eventsA::add);
            sinkB.asFlux().subscribe(eventsB::add);

            CountDownLatch started = new CountDownLatch(2);
            CountDownLatch done = new CountDownLatch(2);
            try (var executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    started.countDown();
                    await(started);
                    ToolEventChannel.setCurrentSessionId("a");
                    try {
                        ToolEventChannel.publish("{\"id\":\"a\"}");
                    } finally {
                        ToolEventChannel.clearCurrentSessionId();
                        done.countDown();
                    }
                });
                executor.submit(() -> {
                    started.countDown();
                    await(started);
                    ToolEventChannel.setCurrentSessionId("b");
                    try {
                        ToolEventChannel.publish("{\"id\":\"b\"}");
                    } finally {
                        ToolEventChannel.clearCurrentSessionId();
                        done.countDown();
                    }
                });
                assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
            }

            assertThat(eventsA).containsExactly("{\"id\":\"a\"}");
            assertThat(eventsB).containsExactly("{\"id\":\"b\"}");
        }
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        void should_complete_flux_when_channel_closed() {
            var sink = ToolEventChannel.open("a");
            ToolEventChannel.clearCurrentSessionId();
            Flux<String> flux = ToolEventChannel.asFlux(sink);

            ToolEventChannel.setCurrentSessionId("a");
            ToolEventChannel.publish("{\"type\":\"tool_call\"}");
            ToolEventChannel.clearCurrentSessionId();
            ToolEventChannel.close("a");

            StepVerifier.create(flux)
                    .expectNext("{\"type\":\"tool_call\"}")
                    .verifyComplete();
        }

        @Test
        void should_ignore_publish_without_current_session() {
            var sink = ToolEventChannel.open("a");
            ToolEventChannel.clearCurrentSessionId();
            List<String> events = new ArrayList<>();
            sink.asFlux().subscribe(events::add);

            ToolEventChannel.publish("{\"leaked\":true}");

            assertThat(events).isEmpty();
            ToolEventChannel.close("a");
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
