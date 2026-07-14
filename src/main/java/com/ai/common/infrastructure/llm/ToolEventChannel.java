package com.ai.common.infrastructure.llm;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Request-scoped sink for tool/source JSON events during chat streaming.
 * Each item is a JSON object string ({@code type}=tool_call|tool_result|sources) sent as SSE data.
 */
public final class ToolEventChannel {

    private static final ThreadLocal<Sinks.Many<String>> HOLDER = new ThreadLocal<>();
    private static final ConcurrentLinkedDeque<Sinks.Many<String>> STACK = new ConcurrentLinkedDeque<>();

    private ToolEventChannel() {}

    public static Sinks.Many<String> open() {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        STACK.push(sink);
        HOLDER.set(sink);
        return sink;
    }

    public static void publish(String jsonPayload) {
        Sinks.Many<String> sink = HOLDER.get();
        if (sink == null) {
            sink = STACK.peek();
        }
        if (sink == null) {
            return;
        }
        sink.tryEmitNext(jsonPayload);
    }

    public static Flux<String> asFlux(Sinks.Many<String> sink) {
        return sink.asFlux();
    }

    public static void close() {
        Sinks.Many<String> sink = HOLDER.get();
        if (sink != null) {
            sink.tryEmitComplete();
            HOLDER.remove();
            STACK.remove(sink);
        } else {
            Sinks.Many<String> top = STACK.poll();
            if (top != null) {
                top.tryEmitComplete();
            }
        }
    }
}
