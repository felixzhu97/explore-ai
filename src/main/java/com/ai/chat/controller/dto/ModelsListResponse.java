package com.ai.chat.controller.dto;

import java.util.List;

public record ModelsListResponse(String provider, List<ModelInfoResponse> models, int count) {
  /** Creates the model list for a provider. */
  public static ModelsListResponse of(String provider, List<ModelInfoResponse> models) {
    return new ModelsListResponse(provider, models, models.size());
  }
}
