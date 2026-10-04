package com.ai.chat.controller.dto;

import com.ai.chat.domain.vo.WebSource;

public record WebSourceResponse(String title, String url, String snippet, String publishedAt) {
  public static WebSourceResponse from(WebSource source) {
    return new WebSourceResponse(
        source.title(), source.url(), source.snippet(), source.publishedAt());
  }
}
