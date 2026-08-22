package com.ai.chat.controller.dto;

import java.util.List;

/** Documentation. */
public record ModelsListResponse(String provider, List<ModelInfoResponse> models, int count) {
  /** Documentation. */
  public static ModelsListResponse of(String provider, List<ModelInfoResponse> models) {
    return new ModelsListResponse(provider, models, models.size());
  }
}
