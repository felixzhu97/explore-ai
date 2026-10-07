package com.ai.common.service.llm;

import java.util.List;

/**
 * Stream event listing the web pages a search tool returned.
 *
 * @param type always {@code sources}
 */
public record WebSourcesEvent(String type, String query, List<Source> items) {

  /** Creates a web sources event. */
  public static WebSourcesEvent createEvent(String query, List<Source> items) {
    return new WebSourcesEvent("sources", query, List.copyOf(items));
  }

  /**
   * One search result.
   *
   * @param publishedAt publisher date as reported by the search provider, or null when unknown
   */
  public record Source(String title, String url, String snippet, String publishedAt) {}
}
