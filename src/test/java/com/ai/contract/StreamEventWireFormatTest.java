package com.ai.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.audio.controller.dto.TranscriptionResponse;
import com.ai.audio.controller.dto.TranscriptionResponse.TranscriptionType;
import com.ai.chat.controller.dto.WebSourceResponse;
import com.ai.chat.domain.model.WebSource;
import com.ai.common.service.llm.StreamTokenEvent;
import com.ai.common.service.llm.ToolCallEvent;
import com.ai.common.service.llm.ToolResultEvent;
import com.ai.common.service.llm.WebSourcesEvent;
import com.ai.pipeline.service.PipelineHandoffEvent;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.service.dto.RagSourceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Stream event wire format")
class StreamEventWireFormatTest {

  private final ObjectMapper json = new ObjectMapper();

  @Test
  @DisplayName("should encode token event with type and raw token")
  void shouldEncodeTokenEventWithTypeAndRawToken() {
    assertThat(StreamTokenEvent.toJson(" hi"))
        .isEqualTo("{\"type\":\"message\",\"token\":\" hi\"}");
  }

  @Test
  @DisplayName("should encode tool call with empty input when model sent none")
  void shouldEncodeToolCallWithEmptyInputWhenModelSentNone() throws Exception {
    assertThat(json.writeValueAsString(ToolCallEvent.of("searchWeb", null)))
        .isEqualTo("{\"type\":\"tool_call\",\"name\":\"searchWeb\",\"input\":\"\"}");
  }

  @Test
  @DisplayName("should encode tool result with ok flag and output")
  void shouldEncodeToolResultWithOkFlagAndOutput() throws Exception {
    assertThat(json.writeValueAsString(ToolResultEvent.failure("searchWeb", "boom")))
        .isEqualTo(
            "{\"type\":\"tool_result\",\"name\":\"searchWeb\",\"ok\":false,\"output\":\"boom\"}");
  }

  @Test
  @DisplayName("should always emit published at as null when date is unknown")
  void shouldAlwaysEmitPublishedAtAsNullWhenDateIsUnknown() throws Exception {
    WebSourcesEvent event =
        WebSourcesEvent.of(
            "spring",
            List.of(new WebSourcesEvent.Source("Spring", "https://spring.io", "s", null)));

    assertThat(json.writeValueAsString(event))
        .isEqualTo(
            "{\"type\":\"sources\",\"query\":\"spring\",\"items\":[{\"title\":\"Spring\","
                + "\"url\":\"https://spring.io\",\"snippet\":\"s\",\"publishedAt\":null}]}");
  }

  @Test
  @DisplayName("should send stored web source without date as null published at")
  void shouldSendStoredWebSourceWithoutDateAsNullPublishedAt() throws Exception {
    WebSourceResponse response =
        WebSourceResponse.from(new WebSource("Spring", "https://spring.io", "s"));

    assertThat(json.writeValueAsString(response))
        .isEqualTo(
            "{\"title\":\"Spring\",\"url\":\"https://spring.io\",\"snippet\":\"s\","
                + "\"publishedAt\":null}");
  }

  @Test
  @DisplayName("should encode rag sources without id and skip blank chunks")
  void shouldEncodeRagSourcesWithoutIdAndSkipBlankChunks() throws Exception {
    List<RagSourceEvent> events =
        RagSourceEvent.fromAll(
            List.of(
                new SourceDocument("chunk", 0.5, Map.of("source", "a.md")),
                new SourceDocument(" ", 0.1, Map.of()),
                new SourceDocument("bare", 0.2, null)));

    assertThat(json.writeValueAsString(events))
        .isEqualTo(
            "[{\"content\":\"chunk\",\"score\":0.5,\"metadata\":{\"source\":\"a.md\"}},"
                + "{\"content\":\"bare\",\"score\":0.2,\"metadata\":{}}]");
  }

  @Test
  @DisplayName("should encode handoff with escaped reason and empty reason when null")
  void shouldEncodeHandoffWithEscapedReasonAndEmptyReasonWhenNull() {
    assertThat(PipelineHandoffEvent.of("k8s", "say \"hi\"").toJson())
        .isEqualTo("{\"agentType\":\"k8s\",\"reason\":\"say \\\"hi\\\"\"}");
    assertThat(PipelineHandoffEvent.of("k8s", null).toJson())
        .isEqualTo("{\"agentType\":\"k8s\",\"reason\":\"\"}");
  }

  @Test
  @DisplayName("should round trip transcription frame with lowercase type")
  void shouldRoundTripTranscriptionFrameWithLowercaseType() throws Exception {
    TranscriptionResponse frame =
        json.readValue("{\"type\":\"partial\",\"text\":\"hel\"}", TranscriptionResponse.class);

    assertThat(frame).isEqualTo(new TranscriptionResponse(TranscriptionType.PARTIAL, "hel"));
    assertThat(json.writeValueAsString(TranscriptionResponse.error("down")))
        .isEqualTo("{\"type\":\"error\",\"text\":\"down\"}");
  }

  @Test
  @DisplayName("should default missing transcription text to empty")
  void shouldDefaultMissingTranscriptionTextToEmpty() throws Exception {
    TranscriptionResponse frame =
        json.readValue("{\"type\":\"final\"}", TranscriptionResponse.class);

    assertThat(frame.text()).isEmpty();
  }

  @Test
  @DisplayName("should reject unknown transcription type")
  void shouldRejectUnknownTranscriptionType() {
    assertThatThrownBy(
            () ->
                json.readValue("{\"type\":\"chunk\",\"text\":\"x\"}", TranscriptionResponse.class))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
  }
}
