package com.ai.common.infra.llm;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Request-scoped sink for tool/source JSON events during chat streaming. Each item is a JSON object
 * string ({@code type}=tool_call|tool_result|sources) sent as SSE data. Channels are keyed by
 * session/request id — never shared via a global stack peek.
 */
public final class ToolEventChannel {

  private static final ConcurrentHashMap<String, Sinks.Many<String>> BY_ID =
      new ConcurrentHashMap<>();
  private static final ThreadLocal<String> CURRENT_ID = new ThreadLocal<>();

  private ToolEventChannel() {}

  /** Opens a buffered sink for the channel id and binds it to the current thread. */
  public static Sinks.Many<String> open(String channelId) {
    Objects.requireNonNull(channelId, "channelId");
    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
    BY_ID.put(channelId, sink);
    CURRENT_ID.set(channelId);
    return sink;
  }

  public static void setCurrentSessionId(String channelId) {
    CURRENT_ID.set(channelId);
  }

  public static void clearCurrentSessionId() {
    CURRENT_ID.remove();
  }

  public static String getCurrentSessionId() {
    return CURRENT_ID.get();
  }

  /** Emits the JSON event to the current thread's channel; no-op when no channel is open. */
  public static void publish(String jsonPayload) {
    String id = CURRENT_ID.get();
    if (id == null) {
      return;
    }
    Sinks.Many<String> sink = BY_ID.get(id);
    if (sink == null) {
      return;
    }
    sink.tryEmitNext(jsonPayload);
  }

  public static Flux<String> asFlux(Sinks.Many<String> sink) {
    return sink.asFlux();
  }

  /** Completes and removes the channel's sink, unbinding it from the current thread if bound. */
  public static void close(String channelId) {
    if (channelId == null) {
      return;
    }
    Sinks.Many<String> sink = BY_ID.remove(channelId);
    if (sink != null) {
      sink.tryEmitComplete();
    }
    if (channelId.equals(CURRENT_ID.get())) {
      CURRENT_ID.remove();
    }
  }
}
