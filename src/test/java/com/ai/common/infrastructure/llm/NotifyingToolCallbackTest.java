package com.ai.common.infrastructure.llm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotifyingToolCallback")
class NotifyingToolCallbackTest {

    @AfterEach
    void tearDown() {
        ToolEventChannel.close();
    }

    @Test
    @DisplayName("should_emit_tool_call_and_result_events_when_delegate_succeeds")
    void should_emit_tool_call_and_result_events_when_delegate_succeeds() {
        var sink = ToolEventChannel.open();
        List<String> events = new ArrayList<>();
        Flux<String> flux = ToolEventChannel.asFlux(sink).doOnNext(events::add);

        ToolCallback delegate = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("searchWeb").description("search").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return "ok:" + toolInput;
            }
        };

        String result = new NotifyingToolCallback(delegate).call("{\"q\":\"hello\"}");
        ToolEventChannel.close();

        assertThat(result).isEqualTo("ok:{\"q\":\"hello\"}");
        StepVerifier.create(flux)
                .expectNextCount(2)
                .verifyComplete();
        assertThat(events.get(0)).contains("\"type\":\"tool_call\"").contains("searchWeb");
        assertThat(events.get(1)).contains("\"type\":\"tool_result\"").contains("\"ok\":true");
    }
}
