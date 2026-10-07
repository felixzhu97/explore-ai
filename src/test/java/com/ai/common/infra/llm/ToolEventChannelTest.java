package com.ai.common.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DisplayName("ToolEventChannel")
class ToolEventChannelTest {

  @AfterEach
  void tearDown() {
    ToolEventChannel.closeChannel("a");
    ToolEventChannel.closeChannel("b");
    ToolEventChannel.clearCurrentSessionId();
  }

  @Test
  void shouldNotCrossPublishBetweenChannels() throws Exception {
    var sinkA = ToolEventChannel.openChannel("a");
    ToolEventChannel.clearCurrentSessionId();
    var sinkB = ToolEventChannel.openChannel("b");
    ToolEventChannel.clearCurrentSessionId();

    List<String> eventsA = new ArrayList<>();
    List<String> eventsB = new ArrayList<>();
    sinkA.asFlux().subscribe(eventsA::add);
    sinkB.asFlux().subscribe(eventsB::add);

    CountDownLatch started = new CountDownLatch(2);
    CountDownLatch done = new CountDownLatch(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      executor.submit(
          () -> {
            started.countDown();
            await(started);
            ToolEventChannel.setCurrentSessionId("a");
            try {
              ToolEventChannel.publishEvent("{\"id\":\"a\"}");
            } finally {
              ToolEventChannel.clearCurrentSessionId();
              done.countDown();
            }
          });
      executor.submit(
          () -> {
            started.countDown();
            await(started);
            ToolEventChannel.setCurrentSessionId("b");
            try {
              ToolEventChannel.publishEvent("{\"id\":\"b\"}");
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

  @Test
  void shouldCompleteFluxWhenChannelClosed() {
    var sink = ToolEventChannel.openChannel("a");
    ToolEventChannel.clearCurrentSessionId();
    final Flux<String> flux = ToolEventChannel.asFlux(sink);

    ToolEventChannel.setCurrentSessionId("a");
    ToolEventChannel.publishEvent("{\"type\":\"tool_call\"}");
    ToolEventChannel.clearCurrentSessionId();
    ToolEventChannel.closeChannel("a");

    StepVerifier.create(flux).expectNext("{\"type\":\"tool_call\"}").verifyComplete();
  }

  @Test
  void shouldIgnorePublishWithoutCurrentSession() {
    var sink = ToolEventChannel.openChannel("a");
    ToolEventChannel.clearCurrentSessionId();
    List<String> events = new ArrayList<>();
    sink.asFlux().subscribe(events::add);

    ToolEventChannel.publishEvent("{\"leaked\":true}");

    assertThat(events).isEmpty();
    ToolEventChannel.closeChannel("a");
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
