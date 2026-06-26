package com.ai.rag.web.dto;

import java.util.List;

/**
 * RAG chat response DTO containing AI response and source documents.
 */
public record RagChatResponse(
    String content,
    List<SourceDocumentDto> sources
) {}
