package com.ai.chat.application.usecase;

import com.ai.chat.domain.vo.WebSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CapturedWebSources")
class CapturedWebSourcesTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("should_remember_and_take_sources_per_channel")
    void should_remember_and_take_sources_per_channel() {
        CapturedWebSources.remember(
                "session-a",
                "who is ceo",
                List.of(new WebSource("Wiki", "https://example.com", "bio")));

        CapturedWebSources.Capture capture = CapturedWebSources.take("session-a");

        assertThat(capture).isNotNull();
        assertThat(capture.query()).isEqualTo("who is ceo");
        assertThat(capture.sources()).hasSize(1);
        assertThat(CapturedWebSources.take("session-a")).isNull();
    }

    @Test
    @DisplayName("should_parse_items_from_sources_payload")
    void should_parse_items_from_sources_payload() {
        ArrayNode items = json.createArrayNode();
        ObjectNode item = items.addObject();
        item.put("title", "T");
        item.put("url", "https://u.example");
        item.put("snippet", "S");

        List<WebSource> parsed = CapturedWebSources.parseItems(items);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.getFirst().url()).isEqualTo("https://u.example");
    }

    @Test
    @DisplayName("should_ignore_invalid_remember_requests")
    void should_ignore_invalid_remember_requests() {
        CapturedWebSources.remember(null, "q", List.of(new WebSource("t", "u", "s")));
        CapturedWebSources.remember(" ", "q", List.of(new WebSource("t", "u", "s")));
        CapturedWebSources.remember("id", "q", List.of());
        assertThat(CapturedWebSources.peek("id")).isNull();
    }

    @Test
    @DisplayName("should_handle_null_channel_on_take_peek_and_clear")
    void should_handle_null_channel_on_take_peek_and_clear() {
        assertThat(CapturedWebSources.take(null)).isNull();
        assertThat(CapturedWebSources.peek(null)).isNull();
        CapturedWebSources.clear(null);
    }

    @Test
    @DisplayName("should_parse_items_with_missing_fields_and_non_array")
    void should_parse_items_with_missing_fields_and_non_array() {
        ObjectNode item = json.createObjectNode();
        item.putNull("title");
        assertThat(CapturedWebSources.parseItems(null)).isEmpty();
        assertThat(CapturedWebSources.parseItems(item)).isEmpty();
        assertThat(CapturedWebSources.parseItems(json.createArrayNode().add(item))).hasSize(1);
    }
}
