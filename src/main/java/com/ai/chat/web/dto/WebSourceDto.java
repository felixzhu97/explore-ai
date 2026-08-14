package com.ai.chat.web.dto;

import com.ai.chat.domain.vo.WebSource;

/** Documentation. */
public record WebSourceDto(String title, String url, String snippet, String publishedAt) {
  /** Documentation. */
  public static WebSourceDto from(WebSource source) {
    return new WebSourceDto(source.title(), source.url(), source.snippet(), source.publishedAt());
  }
}
