package com.ai.chat.domain.model;

import lombok.Value;

/** A cited web search result attached to an assistant reply. */
@Value
public class WebSource {
  String title;
  String url;
  String snippet;
  String publishedAt;

  public WebSource(String title, String url, String snippet, String publishedAt) {
    title = title == null ? "" : title;
    url = url == null ? "" : url;
    snippet = snippet == null ? "" : snippet;
    publishedAt = publishedAt == null ? "" : publishedAt;
    this.title = title;
    this.url = url;
    this.snippet = snippet;
    this.publishedAt = publishedAt;
  }

  public WebSource(String title, String url, String snippet) {
    this(title, url, snippet, "");
  }
}
