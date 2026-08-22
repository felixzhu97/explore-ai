package com.ai.rag.service.dto;

import com.ai.rag.domain.model.SourceDocument;
import java.util.List;

/** Documentation. */
public record RagChatResult(String response, List<SourceDocument> sources) {}
