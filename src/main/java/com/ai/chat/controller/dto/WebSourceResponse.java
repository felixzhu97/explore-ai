package com.ai.chat.controller.dto;

import com.ai.chat.domain.model.WebSource;

/**
 * A cited web search result attached to an assistant reply.
 *
 * @param publishedAt publisher date as reported by the search provider, or null when unknown
 */
public record WebSourceResponse(String title, String url, String snippet, String publishedAt) {

  /** Maps a web source to a response. */
  public static WebSourceResponse from(WebSource source) {
    String publishedAt = source.getPublishedAt().isBlank() ? null : source.getPublishedAt();
    return new WebSourceResponse(
        source.getTitle(), source.getUrl(), source.getSnippet(), publishedAt);
  }
}
