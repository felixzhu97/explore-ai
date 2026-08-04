package com.ai.rag.web;

import java.util.List;

/**
 * Document list response DTO.
 */
public record DocumentListResponse(
    List<DocumentSummaryDto> documents
) {}
