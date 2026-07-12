package com.ai.rag.application.dto;

import com.ai.rag.domain.model.SourceDocument;

import java.util.List;

public record RagChatResult(
        String response,
        List<SourceDocument> sources
) {
}
