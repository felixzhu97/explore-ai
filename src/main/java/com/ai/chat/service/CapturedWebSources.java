package com.ai.chat.service;

import com.ai.chat.domain.vo.WebSource;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Holds the latest sources SSE payload per conversation until afterSessionStream persists it. */
final class CapturedWebSources {

  private static final ConcurrentHashMap<String, Capture> BY_CHANNEL = new ConcurrentHashMap<>();

  private CapturedWebSources() {}

  /** Remembers the web sources found for a stream. */
  static void remember(String channelId, String query, List<WebSource> sources) {
    if (channelId == null || channelId.isBlank() || sources == null || sources.isEmpty()) {
      return;
    }
    BY_CHANNEL.put(channelId, new Capture(query == null ? "" : query, List.copyOf(sources)));
  }

  /** Returns and removes the web sources of a stream. */
  static Capture take(String channelId) {
    if (channelId == null) {
      return null;
    }
    return BY_CHANNEL.remove(channelId);
  }

  /** Returns the web sources of a stream without removing them. */
  static Capture peek(String channelId) {
    if (channelId == null) {
      return null;
    }
    return BY_CHANNEL.get(channelId);
  }

  /** Forgets the web sources of a stream. */
  static void clear(String channelId) {
    if (channelId != null) {
      BY_CHANNEL.remove(channelId);
    }
  }

  /** Parses web search result items into web sources. */
  static List<WebSource> parseItems(JsonNode itemsNode) {
    if (itemsNode == null || !itemsNode.isArray()) {
      return List.of();
    }
    List<WebSource> sources = new ArrayList<>();
    for (JsonNode item : itemsNode) {
      sources.add(
          new WebSource(
              readText(item, "title"),
              readText(item, "url"),
              readText(item, "snippet"),
              readText(item, "publishedAt")));
    }
    return sources;
  }

  private static String readText(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? "" : value.asText("");
  }

  record Capture(String query, List<WebSource> sources) {}
}
