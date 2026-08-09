package com.ai.chat.domain.vo;

/**
 * A cited web search result attached to an assistant reply.
 */
public record WebSource(String title, String url, String snippet, String publishedAt) {

    public WebSource {
        title = title == null ? "" : title;
        url = url == null ? "" : url;
        snippet = snippet == null ? "" : snippet;
        publishedAt = publishedAt == null ? "" : publishedAt;
    }

    public WebSource(String title, String url, String snippet) {
        this(title, url, snippet, "");
    }
}
