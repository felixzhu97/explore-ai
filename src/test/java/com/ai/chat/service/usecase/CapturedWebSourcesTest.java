package com.ai.chat.service.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.chat.domain.vo.WebSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CapturedWebSources")
class CapturedWebSourcesTest {

  private final ObjectMapper json = new ObjectMapper();

  @Test
  @DisplayName("should remember and take sources per channel")
  void shouldRememberAndTakeSourcesPerChannel() {
    CapturedWebSources.remember(
        "session-a", "who is ceo", List.of(new WebSource("Wiki", "https://example.com", "bio")));

    CapturedWebSources.Capture capture = CapturedWebSources.take("session-a");

    assertThat(capture).isNotNull();
    assertThat(capture.query()).isEqualTo("who is ceo");
    assertThat(capture.sources()).hasSize(1);
    assertThat(CapturedWebSources.take("session-a")).isNull();
  }

  @Test
  @DisplayName("should parse items from sources payload")
  void shouldParseItemsFromSourcesPayload() {
    ArrayNode items = json.createArrayNode();
    ObjectNode item = items.addObject();
    item.put("title", "T");
    item.put("url", "https://u.example");
    item.put("snippet", "S");
    item.put("publishedAt", "Jul 19, 2026");

    List<WebSource> parsed = CapturedWebSources.parseItems(items);

    assertThat(parsed).hasSize(1);
    assertThat(parsed.getFirst().url()).isEqualTo("https://u.example");
    assertThat(parsed.getFirst().publishedAt()).isEqualTo("Jul 19, 2026");
  }
}
