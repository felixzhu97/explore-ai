package com.ai.chat.web.dto;

import java.util.List;

public record ModelsListResponse(
        String provider,
        List<ModelInfoResponse> models,
        int count
) {
    public static ModelsListResponse of(String provider, List<ModelInfoResponse> models) {
        return new ModelsListResponse(provider, models, models.size());
    }
}
