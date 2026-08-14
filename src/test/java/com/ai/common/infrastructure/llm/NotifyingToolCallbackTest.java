package com.ai.common.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DisplayName("NotifyingToolCallback")
class NotifyingToolCallbackTest {

  private static final String CHANNEL = "notify-test";

  @AfterEach
  void tearDown() {
    ToolEventChannel.close(CHANNEL);
    ToolEventChannel.clearCurrentSessionId();
  }

  @Test
  @DisplayName("should emit tool call and result events when delegate succeeds")
  void shouldEmitToolCallAndResultEventsWhenDelegateSucceeds() {
    var sink = ToolEventChannel.open(CHANNEL);
    ToolEventChannel.clearCurrentSessionId();
    List<String> events = new ArrayList<>();
    Flux<String> flux = ToolEventChannel.asFlux(sink).doOnNext(events::add);

    ToolCallback delegate =
        new ToolCallback() {
          @Override
          public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("searchWeb")
                .description("search")
                .inputSchema("{}")
                .build();
          }

          @Override
          public String call(String toolInput) {
            return "ok:" + toolInput;
          }
        };

    String result = new NotifyingToolCallback(delegate, CHANNEL).call("{\"q\":\"hello\"}");
    ToolEventChannel.close(CHANNEL);

    assertThat(result).isEqualTo("ok:{\"q\":\"hello\"}");
    StepVerifier.create(flux).expectNextCount(2).verifyComplete();
    assertThat(events.get(0)).contains("\"type\":\"tool_call\"").contains("searchWeb");
    assertThat(events.get(1)).contains("\"type\":\"tool_result\"").contains("\"ok\":true");
  }
}
