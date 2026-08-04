package com.ai.rag.application;

import com.ai.rag.domain.SourceDocument;

import java.util.List;

public record RagChatResult(
        String response,
        List<SourceDocument> sources
) {
}
