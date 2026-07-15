package com.ai.chat.application.usecase;

import com.ai.chat.domain.vo.WebSource;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the latest sources SSE payload per conversation until afterSessionStream persists it.
 */
final class CapturedWebSources {

    private static final ConcurrentHashMap<String, Capture> BY_CHANNEL = new ConcurrentHashMap<>();

    private CapturedWebSources() {
    }

    static void remember(String channelId, String query, List<WebSource> sources) {
        if (channelId == null || channelId.isBlank() || sources == null || sources.isEmpty()) {
            return;
        }
        BY_CHANNEL.put(channelId, new Capture(query == null ? "" : query, List.copyOf(sources)));
    }

    static Capture take(String channelId) {
        if (channelId == null) {
            return null;
        }
        return BY_CHANNEL.remove(channelId);
    }

    static void clear(String channelId) {
        if (channelId != null) {
            BY_CHANNEL.remove(channelId);
        }
    }

    static List<WebSource> parseItems(JsonNode itemsNode) {
        if (itemsNode == null || !itemsNode.isArray()) {
            return List.of();
        }
        List<WebSource> sources = new ArrayList<>();
        for (JsonNode item : itemsNode) {
            sources.add(new WebSource(
                    text(item, "title"),
                    text(item, "url"),
                    text(item, "snippet")));
        }
        return sources;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    record Capture(String query, List<WebSource> sources) {
    }
}
